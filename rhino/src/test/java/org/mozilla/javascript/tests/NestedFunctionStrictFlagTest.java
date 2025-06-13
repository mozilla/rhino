package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.testutils.Utils;

class NestedFunctionStrictFlagTest {
    @Test
    void functionNestedInAStrictFunctionAreStrict() {
        String script = "(() => {\n  'use strict';\n  return function nested() {}\n})()";

        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    TopLevel topLevel = new TopLevel();
                    cx.initStandardObjects(topLevel);

                    Object res = cx.evaluateString(topLevel, script, "test", 1, null);
                    NativeFunction function = assertInstanceOf(NativeFunction.class, res);
                    assertTrue(
                            function.isStrict(),
                            () ->
                                    "should be strict in "
                                            + (cx.isInterpretedMode() ? "interpreted" : "compiled")
                                            + " mode");

                    return null;
                });
    }
}
