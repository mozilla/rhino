package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSConstructor;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.tests.Utils;

public class NativeFunctionTest {

    @Test
    public void functionPrototypeLength() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Scriptable scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "var desc=Object.getOwnPropertyDescriptor(Function.prototype, 'length');\n"
                                            + "var res = 'configurable: ' + desc.configurable;\n"
                                            + "res += '  enumerable: ' + desc.enumerable;\n"
                                            + "res += '  writable: ' + desc.writable;",
                                    "test",
                                    1,
                                    null);
                    assertEquals("configurable: true  enumerable: false  writable: false", result);

                    return null;
                });
    }

    @Test
    public void functionPrototypeName() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Scriptable scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "var desc=Object.getOwnPropertyDescriptor(Function.prototype, 'name');\n"
                                            + "var res = 'configurable: ' + desc.configurable;\n"
                                            + "res += '  enumerable: ' + desc.enumerable;\n"
                                            + "res += '  writable: ' + desc.writable;",
                                    "test",
                                    1,
                                    null);
                    assertEquals("configurable: true  enumerable: false  writable: false", result);

                    return null;
                });
    }

    @Test
    public void functionLength() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Scriptable scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "var f=function(){};\n"
                                            + "var desc=Object.getOwnPropertyDescriptor(f, 'length');\n"
                                            + "var res = 'configurable: ' + desc.configurable;\n"
                                            + "res += '  enumerable: ' + desc.enumerable;\n"
                                            + "res += '  writable: ' + desc.writable;",
                                    "test",
                                    1,
                                    null);
                    assertEquals("configurable: true  enumerable: false  writable: false", result);

                    return null;
                });
    }

    @Test
    public void functionName() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Scriptable scope = cx.initStandardObjects();

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "var f=function(){};\n"
                                            + "var desc=Object.getOwnPropertyDescriptor(f, 'name');\n"
                                            + "var res = 'configurable: ' + desc.configurable;\n"
                                            + "res += '  enumerable: ' + desc.enumerable;\n"
                                            + "res += '  writable: ' + desc.writable;",
                                    "test",
                                    1,
                                    null);
                    assertEquals("configurable: true  enumerable: false  writable: false", result);

                    return null;
                });
    }

    @Test
    public void functionNameJavaObject() throws Exception {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    final Scriptable scope = cx.initStandardObjects();
                    try {
                        ScriptableObject.defineClass(scope, HelperObject.class);
                    } catch (Exception e) {
                    }

                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "var f=new HelperObject().foo;\n"
                                            + "var desc=Object.getOwnPropertyDescriptor(f, 'name');\n"
                                            + "var res = 'configurable: ' + desc.configurable;\n"
                                            + "res += '  enumerable: ' + desc.enumerable;\n"
                                            + "res += '  writable: ' + desc.writable;",
                                    "test",
                                    1,
                                    null);
                    assertEquals("configurable: true  enumerable: false  writable: false", result);

                    return null;
                });
    }

    public static class HelperObject extends ScriptableObject {

        public HelperObject() {}

        @Override
        public String getClassName() {
            return "HelperObject";
        }

        @JSConstructor
        public void jsConstructorMethod() {
            put("initialized", this, Boolean.TRUE);
        }

        @JSFunction("foo")
        public Object foo() {
            return "foo()";
        }
    }
}
