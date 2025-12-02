/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.javascript.tests.es5;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/** Test for let. */
class LetTest {

    @Test
    void simple() {
        String script =
                "function f(a,b,c) {\n"
                        + " let sum = a + b + c;\n"
                        + " return sum;\n"
                        + "}\n"
                        + "f(1, 2, 3);";
        Utils.assertWithAllModes_1_8(6, script);
    }

    @Test
    void switchLet() {
        String script =
                "var sum = 0;\n"
                        + "var t = 1;\n"
                        + "switch(t) {\n"
                        + "  case 1:\n"
                        + "    let s = t + 2;\n"
                        + "    sum += s + 3;\n"
                        + "    break;\n"
                        + "  default:\n"
                        + "    sum = 7;\n"
                        + "}\n"
                        + "sum;";
        Utils.assertWithAllModes_1_8(6, script);
    }

    @Test
    void forSwitchLet() {
        String script =
                "  var sum = 0;\n"
                        + "for (let i = 0; i < 1; i++)\n"
                        + "  switch (i) {\n"
                        + "    case 0:\n"
                        + "      let test = 7;\n"
                        + "      sum += 4;\n"
                        + "      break;\n"
                        + "    }"
                        + "sum;";
        Utils.assertWithAllModes_1_8(4, script);
    }

    @Test
    void ifSwitchLet() {
        String script =
                "var sum = 0;\n"
                        + "if (sum == 0)\n"
                        + "  switch (sum) {\n"
                        + "    case 0:\n"
                        + "      let test = 7;\n"
                        + "      sum += 4;\n"
                        + "      break;\n"
                        + "    }"
                        + "sum;";
        Utils.assertWithAllModes_1_8(4, script);
    }

    @Test
    void letInsideBodyOfSwitch() {
        String script =
                "switch (0) {\n"
                        + "  default:\n"
                        + "    let f;\n"
                        + "  {\n"
                        + "  }\n"
                        + "}\n"
                        + "\n"
                        + "typeof f;\n";
        Utils.assertWithAllModes_1_8("undefined", script);
    }

    @Test
    void letInsideBodyOfSwitchES6Strict() {
        String script =
                "(function () {\n"
                        + "  'use strict';\n"
                        + "  switch (true) {\n"
                        + "    case true:\n"
                        + "      let x = 1;\n"
                        + "      return x;\n"
                        + "  }\n"
                        + "})()\n";
        Utils.assertWithAllModes_ES6(1, script);
    }

    @Test
    void letInsideFunctionSwitchIf() {
        String script =
                "(function () {\n"
                        + "    switch (0) {\n"
                        + "        default:\n"
                        + "            let a = 0;\n"
                        + "            if (a == 0) {\n"
                        + "                a++;\n"
                        + "            }\n"
                        + "            return a;\n"
                        + "    }\n"
                        + "})();\n";
        Utils.assertWithAllModes_1_8(1, script);
    }

    @Test
    void letInsideFunctionWhileIf() {
        String script =
                "(function () {\n"
                        + "    while (true) {\n"
                        + "            let a = 0;\n"
                        + "            if (a == 0) {\n"
                        + "                a++;\n"
                        + "            }\n"
                        + "            return a;\n"
                        + "    }\n"
                        + "})();\n";
        Utils.assertWithAllModes_1_8(1, script);
    }
}
