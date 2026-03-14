/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.javascript.tests.es5;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/** Tests for automatic semicolon insertion (ASI) with variable declarations. */
class VariableDeclAsiTest {

    @Test
    void letNoSemicolonSameLine() {
        Utils.assertEvaluatorException_1_8("missing ; before statement", "let a = 4 let b = 5");
    }

    @Test
    void letAutoSemicolonInsertionNewline() {
        Utils.assertWithAllModes_1_8(9, "let a = 4\nlet b = 5\na + b");
    }

    @Test
    void constNoSemicolonSameLine() {
        Utils.assertEvaluatorException_1_8("missing ; before statement", "const a = 4 const b = 5");
    }

    @Test
    void constAutoSemicolonInsertionNewline() {
        Utils.assertWithAllModes_1_8(9, "const a = 4\nconst b = 5\na + b");
    }

    @Test
    void varNoSemicolonSameLine() {
        Utils.assertEvaluatorException_1_8("missing ; before statement", "var a = 4 var b = 5");
    }

    @Test
    void varAutoSemicolonInsertionNewline() {
        Utils.assertWithAllModes_1_8(9, "var a = 4\nvar b = 5\na + b");
    }
}
