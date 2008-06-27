/**
 * 
 */
package org.mozilla.javascript.tests;

import java.io.*;

import junit.framework.TestCase;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ContinuationPending;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrappedException;
import org.mozilla.javascript.serialize.*;

/**
 * @author Norris Boyd
 */
public class ContinuationsApiTest extends TestCase {
  Scriptable globalScope;
    
  public static class MyClass {
      public int f(int a) {
          Context cx = Context.enter();
          try {
              ContinuationPending pending = cx.captureContinuation();
              pending.setApplicationState(a);
              throw pending;
          } finally {
              Context.exit();
          }
      }

      public int g(int a) {
          Context cx = Context.enter();
          try {
              ContinuationPending pending = cx.captureContinuation();
              pending.setApplicationState(2*a);
              throw pending;
          } finally {
              Context.exit();
          }
      }
  }

  @Override
  public void setUp() {
      Context cx = Context.enter();
      try {
          globalScope = cx.initStandardObjects();
          cx.setOptimizationLevel(-1); // must use interpreter mode
          globalScope.put("myObject", globalScope,
                  Context.javaToJS(new MyClass(), globalScope));
      } finally {
          Context.exit();
      }
  }
  
  public void testScriptWithContinuations() {
      Context cx = Context.enter();
      try {
          cx.setOptimizationLevel(-1); // must use interpreter mode
          Script script = cx.compileString("myObject.f(3) + 1;", 
                  "test source", 1, null);
          cx.executeScriptWithContinuations(script, globalScope);
          fail("Should throw ContinuationPending");
      } catch (ContinuationPending pending) {

          Object applicationState = pending.getApplicationState();
          assertEquals(new Integer(3), applicationState);
          int saved = (Integer) applicationState;
          Object result = cx.resumeContinuation(pending, saved + 1);
          assertEquals(5, ((Number)result).intValue());

      } finally {
          Context.exit();
      }
  }

  public void testScriptWithMultipleContinuations() {
      Context cx = Context.enter();
      try {
          cx.setOptimizationLevel(-1); // must use interpreter mode
          Script script = cx.compileString("myObject.f(3) + myObject.g(3) + 2;", 
                  "test source", 1, null);
          cx.executeScriptWithContinuations(script, globalScope);
          fail("Should throw ContinuationPending");
      } catch (ContinuationPending pending) {
          try {
              Object applicationState = pending.getApplicationState();
              assertEquals(new Integer(3), applicationState);
              int saved = (Integer) applicationState;
              cx.resumeContinuation(pending, saved + 1);
              fail("Should throw another ContinuationPending");
          } catch (ContinuationPending pending2) {
              Object applicationState2 = pending2.getApplicationState();
              assertEquals(new Integer(6), applicationState2);
              int saved2 = (Integer) applicationState2;
              Object result2 = cx.resumeContinuation(pending2, saved2 + 1);
              assertEquals(13, ((Number)result2).intValue());
          }
      } finally {
          Context.exit();
      }
  }
  
  public void testFunctionWithContinuations() {
      Context cx = Context.enter();
      try {
          cx.setOptimizationLevel(-1); // must use interpreter mode
          cx.evaluateString(globalScope, 
                  "function f(a) { return myObject.f(a); }",
                  "function test source", 1, null);
          Function f = (Function) globalScope.get("f", globalScope);
          Object[] args = { 7 };
          cx.callFunctionWithContinuations(f, globalScope, args);
          fail("Should throw ContinuationPending");
      } catch (ContinuationPending pending) {
          Object applicationState = pending.getApplicationState();
          assertEquals(7, ((Number)applicationState).intValue());
          int saved = (Integer) applicationState;
          Object result = cx.resumeContinuation(pending, saved + 1);
          assertEquals(8, ((Number)result).intValue());
      } finally {
          Context.exit();
      }
  }
  
  /**
   * Since a continuation can only capture JavaScript frames and not Java
   * frames, ensure that Rhino throws an exception when the JavaScript frames
   * don't reach all the way to the code called by
   * executeScriptWithContinuations or callFunctionWithContinuations.
   */
  public void testErrorOnEvalCall() {
      Context cx = Context.enter();
      try {
          cx.setOptimizationLevel(-1); // must use interpreter mode
          Script script = cx.compileString("eval('myObject.f(3);');",
                  "test source", 1, null);
          cx.executeScriptWithContinuations(script, globalScope);
          fail("Should throw IllegalStateException");
      } catch (WrappedException we) {
          Throwable t = we.getWrappedException();
          assertTrue(t instanceof IllegalStateException);
          assertTrue(t.getMessage().startsWith("Cannot capture continuation"));
      } finally {
          Context.exit();
      }
  }

  public void testSerializationWithContinuations()
      throws IOException, ClassNotFoundException
  {
      Context cx = Context.enter();
      try {
          cx.setOptimizationLevel(-1); // must use interpreter mode
          cx.evaluateString(globalScope, 
                  "function f(a) { return myObject.f(a); }",
                  "function test source", 1, null);
          Function f = (Function) globalScope.get("f", globalScope);
          Object[] args = { 7 };
          cx.callFunctionWithContinuations(f, globalScope, args);
          fail("Should throw ContinuationPending");
      } catch (ContinuationPending pending) {
          // serialize
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          ScriptableOutputStream oos =
              new ScriptableOutputStream(baos, globalScope);
          oos.writeObject(pending);
          oos.close();
          baos.close();
          byte[] serializedData = baos.toByteArray();
          
          // deserialize
          ByteArrayInputStream bais = new ByteArrayInputStream(serializedData);
          ScriptableInputStream ois = new ScriptableInputStream(bais,
                  globalScope);
          ContinuationPending pending2 = (ContinuationPending) ois.readObject();
          ois.close();
          bais.close();
          
          Object applicationState = pending2.getApplicationState();
          assertEquals(7, ((Number)applicationState).intValue());
          int saved = (Integer) applicationState;
          Object result = cx.resumeContinuation(pending2, saved + 1);
          assertEquals(8, ((Number)result).intValue());
      } finally {
          Context.exit();
      }
  }
}
