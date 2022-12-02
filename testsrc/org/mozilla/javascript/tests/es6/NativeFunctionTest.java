package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSConstructor;
import org.mozilla.javascript.annotations.JSFunction;

public class NativeFunctionTest {
    private Context cx;
    private ScriptableObject scope;

    @Before
    public void setUp() throws Exception {
        cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
        scope = cx.initStandardObjects();
        ScriptableObject.defineClass(scope, HelperObject.class);
    }

    @After
    public void tearDown() {
        Context.exit();
    }

    @Test
    public void testFunctionPrototypeLength() {
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
    }

    @Test
    public void testFunctionPrototypeName() {
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
    }

    @Test
    public void testFunctionLength() {
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
    }

    @Test
    public void testFunctionName() {
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
    }

    @Test
    public void testFunctionNameJavaObject() {
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
