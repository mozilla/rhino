package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

public class NativeFunctionTest {
    private Context cx;
    private ScriptableObject scope;

    @Before
    public void setUp() {
        cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
        scope = cx.initStandardObjects();
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
                                + "var res = 'configurable: ' + desc.configurable;"
                                + "res += '  enumerable: ' + desc.enumerable;"
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
                                + "var res = 'configurable: ' + desc.configurable;"
                                + "res += '  enumerable: ' + desc.enumerable;"
                                + "res += '  writable: ' + desc.writable;",
                        "test",
                        1,
                        null);
        assertEquals("configurable: true  enumerable: false  writable: false", result);
    }
}
