package org.mozilla.javascript;

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
}
