/**
 * 
 */
package org.mozilla.javascript.tests;

import junit.framework.TestCase;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.EvaluatorException;

/**
 * @author Norris Boyd
 */
public class ClassShutterExceptionTest extends TestCase {

    /**
     * Define a ClassShutter that prevents access to all Java classes.
     */
    static class OpaqueShutter implements ClassShutter {
        public boolean visibleToScripts(String name) {
            System.err.println("OpaqueShutter asked for " + name);
            return false;
        }
    }

    public void testClassShutterException() {
        Context cx = Context.enter();
        try {
            Scriptable globalScope = cx.initStandardObjects();
            cx.setClassShutter(new OpaqueShutter());
            cx.evaluateString(globalScope,
                    "java.lang.System.out.println('hi');",
                    "test source", 1, null);
            fail();
        } catch (RhinoException e) {
            // OpaqueShutter should prevent access to java.lang...
            return;
        } finally {
            Context.exit();
        }
    }

    public void testThrowingException() {
        Context cx = Context.enter();
        try {
            Scriptable globalScope = cx.initStandardObjects();
            cx.setClassShutter(new OpaqueShutter());
            // JavaScript exceptions with no reference to Java
            // should not be affected by the ClassShutter
            cx.evaluateString(globalScope,
                    "try { throw 3; } catch (e) { }",
                    "test source", 1, null);
        } finally {
            Context.exit();
        }
    }

    public void testThrowingEcmaError() {
        Context cx = Context.enter();
        try {
            Scriptable globalScope = cx.initStandardObjects();
            cx.setClassShutter(new OpaqueShutter());
            // JavaScript exceptions with no reference to Java
            // should not be affected by the ClassShutter
            cx.evaluateString(globalScope,
                    "friggin' syntax error!",
                    "test source", 1, null);
            fail("Should have thrown an exception");
        } catch (EvaluatorException e) {
            // should have thrown an exception for syntax error
        } finally {
            Context.exit();
        }
    }

    public void testThrowingEvaluatorException() {
        Context cx = Context.enter();
        try {
            Scriptable globalScope = cx.initStandardObjects();
            cx.setClassShutter(new OpaqueShutter());
            // JavaScript exceptions with no reference to Java
            // should not be affected by the ClassShutter
            cx.evaluateString(globalScope,
                    "try { eval('for;if;else'); } catch (e) { }",
                    "test source", 1, null);
        } finally {
            Context.exit();
        }
    }
 }
