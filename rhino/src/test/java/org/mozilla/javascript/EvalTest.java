package org.mozilla.javascript;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

class EvalTest {
    @Test
    void evalFunctionIsUndefined() {
        Utils.runWithAllModes(
                cx -> {
                    var scope = cx.initStandardObjects(new TopLevel());
                    Object result =
                            cx.evaluateString(scope, "eval('function f(){}')", "test", 1, null);
                    assertTrue(Undefined.isUndefined(result));
                    return null;
                });
    }
}
