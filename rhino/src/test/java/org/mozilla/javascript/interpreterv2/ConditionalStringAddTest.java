package org.mozilla.javascript.interpreterv2;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Regression test for conditional string add operations in InterpreterV2. Tests the fix for
 * conditional string concatenation that previously broke.
 */
class ConditionalStringAddTest {

    @Test
    void testConditionalStringAdd() {
        String script =
                "var args = \"\";\n"
                        + "var i = 2;\n"
                        + "args += ( i == 2 ) ? i : i + ', ';\n"
                        + "args;";

        Utils.assertWithAllModes_ES6("2", script);
    }

    @Test
    void testConditionalStringAddWithElse() {
        String script =
                "var args = \"\";\n"
                        + "var i = 3;\n"
                        + "args += ( i == 2 ) ? i : i + ', ';\n"
                        + "args;";

        Utils.assertWithAllModes_ES6("3, ", script);
    }

    @Test
    void testConditionalStringAddChained() {
        String script =
                "var args = \"start\";\n"
                        + "for (var i = 1; i <= 3; i++) {\n"
                        + "    args += ( i == 1 ) ? i : ', ' + i;\n"
                        + "}\n"
                        + "args;";

        Utils.assertWithAllModes_ES6("start1, 2, 3", script);
    }

    @Test
    void testConditionalStringAddWithComplexExpression() {
        String script =
                "var result = \"\";\n"
                        + "var x = 5;\n"
                        + "result += (x > 3) ? \"big: \" + x : \"small: \" + x;\n"
                        + "result;";

        Utils.assertWithAllModes_ES6("big: 5", script);
    }
}
