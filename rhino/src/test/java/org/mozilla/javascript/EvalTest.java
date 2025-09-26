package org.mozilla.javascript;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

class EvalTest {
    @Test
    void evalFunctionIsUndefinedInEs6() {
        Utils.assertWithAllModes_ES6("undefined", "typeof eval('function f(){}')");
    }

    @Test
    void evalFunctionReturnsFunctionInOlderVersions() {
        Utils.assertWithAllModes_1_8("function", "typeof eval('function f(){}')");
    }
}
