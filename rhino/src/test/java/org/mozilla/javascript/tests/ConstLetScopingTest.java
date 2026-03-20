/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/** Tests for correct block-scoping of {@code const} and fix of the dead {@code if} guard. */
class ConstLetScopingTest {

    // --- const block-scoping in ES6 ---

    @Test
    void constInBlockIsBlockScoped() {
        // const inside an explicit block must not be visible outside it
        Utils.assertWithAllModes_ES6(42.0, "var result; { const x = 42; result = x; } result;");
    }

    @Test
    void constShadowingOuterConstInInnerBlockIsAllowed() {
        // inner const x shadows outer const x — must not produce a redeclaration error
        Utils.assertWithAllModes_ES6(2.0, "const x = 1; { const x = 2; x; }");
    }

    @Test
    void constRedeclarationInSameScopeIsError() {
        // Two const declarations for the same name in the same scope must be an error
        Utils.assertEvaluatorExceptionES6("redeclaration of const x", "const x = 1; const x = 2;");
    }

    @Test
    void varRedeclarationAcrossBlocksIsAllowed() {
        // var hoists to function scope, so redeclaring in a nested block must be fine
        Utils.assertWithAllModes_ES6(2.0, "var x = 1; { var x = 2; } x;");
    }

    // --- single-statement const/let errors in ES6 ---

    @Test
    void constInSingleStatementForBodyIsError() {
        Utils.assertEvaluatorExceptionES6(
                "const declarations may only appear at the top level or within a block",
                "for (;;) const x = 1;");
    }

    @Test
    void letInSingleStatementIfBodyIsError() {
        Utils.assertEvaluatorExceptionES6(
                "let declaration not directly within block", "if (true) let x = 1;");
    }

    @Test
    void constInSingleStatementIfBodyIsError() {
        Utils.assertEvaluatorExceptionES6(
                "const declarations may only appear at the top level or within a block",
                "if (true) const x = 1;");
    }

    @Test
    void letInSingleStatementElseBodyIsError() {
        Utils.assertEvaluatorExceptionES6(
                "let declaration not directly within block", "if (false) 0; else let x = 1;");
    }

    @Test
    void constInSingleStatementElseBodyIsError() {
        Utils.assertEvaluatorExceptionES6(
                "const declarations may only appear at the top level or within a block",
                "if (false) 0; else const x = 1;");
    }

    @Test
    void letInsideBlockBodyOfIfIsAllowed() {
        Utils.assertWithAllModes_ES6(1.0, "if (true) { let x = 1; x; }");
    }

    @Test
    void constInsideBlockBodyOfIfIsAllowed() {
        Utils.assertWithAllModes_ES6(1.0, "if (true) { const x = 1; x; }");
    }

    // --- pre-ES6 regression: no new errors at VERSION_1_8 ---

    @Test
    void constInSingleStatementIfBodyIsAllowedPreES6() {
        // In pre-ES6 mode the parser must not reject this
        Utils.assertEvaluatorException_1_8("redeclaration of const x", "const x = 1; const x = 2;");
    }

    @Test
    void constShadowingIsErrorPreES6() {
        // Pre-ES6: const is function-scoped, so inner const x is a redeclaration error
        Utils.assertEvaluatorException_1_8(
                "redeclaration of const x", "const x = 1; { const x = 2; }");
    }
}
