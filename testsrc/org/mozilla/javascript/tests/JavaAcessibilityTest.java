/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/**
 *
 */
package org.mozilla.javascript.tests;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.drivers.TestUtils;
import org.mozilla.javascript.tools.shell.Global;
import org.mozilla.javascript.tools.shell.ShellContextFactory;

import junit.framework.TestCase;

/**
 * @author donnamalayeri
 */
public class JavaAcessibilityTest extends TestCase {

  protected final Global global = new Global();
  String importClass = "importClass(Packages.org.mozilla.javascript.tests.PrivateAccessClass)\n";

  public JavaAcessibilityTest() {
    global.init(contextFactory);
  }

  @Override
  protected void setUp() {
    TestUtils.setGlobalContextFactory(contextFactory);
  }

  @Override
  protected void tearDown() {
    TestUtils.setGlobalContextFactory(null);
  }

  private ContextFactory contextFactory = new ShellContextFactory() {
    @Override
    protected boolean hasFeature(Context cx, int featureIndex) {
      if (featureIndex == Context.FEATURE_ENHANCED_JAVA_ACCESS)
            return true;
      return super.hasFeature(cx, featureIndex);
    }
  };

  public void testAccessingFields() {
    Object result = runScript(importClass + "PrivateAccessClass.staticPackagePrivateInt");
    assertEquals(new Integer(0), result);

    result = runScript(importClass + "PrivateAccessClass.staticPrivateInt");
    assertEquals(new Integer(1), result);

    result = runScript(importClass + "PrivateAccessClass.staticProtectedInt");
    assertEquals(new Integer(2), result);

    result = runScript(importClass + "new PrivateAccessClass().packagePrivateString");
    assertEquals("package private", ((NativeJavaObject) result).unwrap());

    result = runScript(importClass + "new PrivateAccessClass().privateString");
    assertEquals("private", ((NativeJavaObject) result).unwrap());

    result = runScript(importClass + "new PrivateAccessClass().protectedString");
    assertEquals("protected", ((NativeJavaObject) result).unwrap());

    result = runScript(importClass + "new PrivateAccessClass.PrivateNestedClass().packagePrivateInt");
    assertEquals(new Integer(0), result);

    result = runScript(importClass + "new PrivateAccessClass.PrivateNestedClass().privateInt");
    assertEquals(new Integer(1), result);

    result = runScript(importClass + "new PrivateAccessClass.PrivateNestedClass().protectedInt");
    assertEquals(new Integer(2), result);
  }

  public void testAccessingMethods() {
    Object result = runScript(importClass + "PrivateAccessClass.staticPackagePrivateMethod()");
    assertEquals(new Integer(0), result);

    result = runScript(importClass + "PrivateAccessClass.staticPrivateMethod()");
    assertEquals(new Integer(1), result);

    result = runScript(importClass + "PrivateAccessClass.staticProtectedMethod()");
    assertEquals(new Integer(2), result);

    result = runScript(importClass + "new PrivateAccessClass().packagePrivateMethod()");
    assertEquals(new Integer(3), result);

    result = runScript(importClass + "new PrivateAccessClass().privateMethod()");
    assertEquals(new Integer(4), result);

    result = runScript(importClass + "new PrivateAccessClass().protectedMethod()");
    assertEquals(new Integer(5), result);
  }

  public void testAccessingConstructors() {
    runScript(importClass + "new PrivateAccessClass(\"foo\")");
    runScript(importClass + "new PrivateAccessClass(5)");
    runScript(importClass + "new PrivateAccessClass(5, \"foo\")");
  }

  public void testAccessingJavaBeanProperty() {
      Object result = runScript(importClass +
          "var x = new PrivateAccessClass(); x.javaBeanProperty + ' ' + x.getterCalled;");
      assertEquals("6 true", result);

      result = runScript(importClass +
      "var x = new PrivateAccessClass(); x.javaBeanProperty = 4; x.javaBeanProperty + ' ' + x.setterCalled;");
      assertEquals("4 true", result);
      
      // assume javaObjectProperty is 'false'
      result = runScript(importClass +
              "var x = new PrivateAccessClass(); x.javaObjectProperty = x.javaObjectProperty || true; x.javaObjectProperty + ' ' + x.setterCalled;");
      assertEquals("true true", result);

      // performs a bitxor, so integer/number is returned;
      result = runScript(importClass +
              "var x = new PrivateAccessClass(); x.javaObjectProperty ^= true; x.javaObjectProperty + ' ' + x.setterCalled;");
      assertEquals("1 true", result);

      // assume javaObjectProperty is '0'
      result = runScript(importClass +
              "var x = new PrivateAccessClass(); x.javaObjectProperty = x.javaObjectProperty + 7; x.javaObjectProperty + ' ' + x.setterCalled;");
      assertEquals("7 true", result);

      // perform simple addition, shoud return "10" and not "3" + "7" = "37"
      result = runScript(importClass +
              "var x = new PrivateAccessClass(); x.javaObjectProperty = 3; x.javaObjectProperty = x.javaObjectProperty + 7; x.javaObjectProperty + ' ' + x.setterCalled;");
      assertEquals("10 true", result);
  }

  public void testOverloadFunctionRegression() {
      Object result = runScript(
        "(new java.util.GregorianCalendar()).set(3,4);'success';");
      assertEquals("success", result);
  }


  private Object runScript(final String scriptSourceText) {
    return contextFactory.call(context -> {
        Script script = context.compileString(scriptSourceText, "", 1, null);
        return script.exec(context, global);
    });
  }
}
