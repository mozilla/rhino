package org.mozilla.javascript;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

public class SymbolHasInstanceTest {
    /** custom Symbol.hasInstance must be called */
    @Test
    public void testInstanceofUsesCustomSymbolHasInstance() {
        String script =
                "function MyClass() {}\n"
                        + "Object.defineProperty(MyClass, Symbol.hasInstance, {\n"
                        + "  value: function(obj) { return Array.isArray(obj); }\n"
                        + "});\n"
                        + "[] instanceof MyClass;";

        Utils.assertWithAllModes_ES6(true, script);
    }

    /** non-function objects with Symbol.hasInstance should receive correct argument */
    @Test
    public void testNonFunctionSymbolHasInstanceReceivesCorrectArgument() {
        String script =
                "var receivedArg;\n"
                        + "var obj = {\n"
                        + "  [Symbol.hasInstance]: function(instance) {\n"
                        + "    receivedArg = instance;\n"
                        + "    return true;\n"
                        + "  }\n"
                        + "};\n"
                        + "var testInstance = {marker: 'test'};\n"
                        + "Object.setPrototypeOf(testInstance, obj);\n"
                        + "testInstance instanceof obj;\n"
                        + "receivedArg === testInstance;";

        Utils.assertWithAllModes_ES6(true, script);
    }

    /** instanceof walks the prototype chain to find Symbol.hasInstance */
    @Test
    public void testSymbolHasInstanceInherited() {
        String script =
                "var called = false;\n"
                        + "function BaseFunc() {}\n"
                        + "Object.defineProperty(BaseFunc, Symbol.hasInstance, {\n"
                        + "  value: function() { called = true; return true; }\n"
                        + "});\n"
                        + "function SubFunc() {}\n"
                        + "Object.setPrototypeOf(SubFunc, BaseFunc);\n"
                        + "var result = ({}) instanceof SubFunc;\n"
                        + "called && result;";
        Utils.assertWithAllModes_ES6(true, script);
    }

    /** errors thrown by custom Symbol.hasInstance should be propagated */
    @Test
    public void testSymbolHasInstanceErrorPropagated() {
        Context cx = Context.enter();
        try {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();

            String script =
                    "function MyClass() {}\n"
                            + "Object.defineProperty(MyClass, Symbol.hasInstance, {\n"
                            + "  value: function(obj) { throw new TypeError('custom error'); }\n"
                            + "});\n"
                            + "{} instanceof MyClass;";

            boolean exceptionThrown = false;
            try {
                cx.evaluateString(scope, script, "test", 1, null);
            } catch (RhinoException e) {
                // Exception was propagated (not swallowed)
                exceptionThrown = true;
            }
            assertTrue(
                    "Expected exception to be propagated from custom Symbol.hasInstance",
                    exceptionThrown);
        } finally {
            Context.exit();
        }
    }
}
