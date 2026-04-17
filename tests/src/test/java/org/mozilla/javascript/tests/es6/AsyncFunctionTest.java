/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/** Tests for async function restrictions. */
public class AsyncFunctionTest {

    @Test
    public void awaitVarInAsyncFunctionError() {
        Utils.assertEvaluatorExceptionES6(
                "identifier is a reserved word: await", "async function f() { var await = 1; }");
    }

    @Test
    public void awaitLetInAsyncFunctionError() {
        Utils.assertEvaluatorExceptionES6(
                "identifier is a reserved word: await", "async function f() { let await = 1; }");
    }

    @Test
    public void awaitConstInAsyncFunctionError() {
        Utils.assertEvaluatorExceptionES6(
                "identifier is a reserved word: await", "async function f() { const await = 1; }");
    }

    @Test
    public void awaitParamInAsyncFunctionError() {
        Utils.assertEvaluatorExceptionES6(
                "identifier is a reserved word: await", "async function f(await) {}");
    }

    @Test
    public void awaitLabelInAsyncFunctionError() {
        // 'await' inside async functions is parsed as an await expression,
        // so 'await:' causes a syntax error (not a label error)
        Utils.assertEvaluatorExceptionES6(
                "syntax error", "async function f() { await: while(true) { break await; } }");
    }

    @Test
    public void awaitAsIdentifierInVoidExprError() {
        Utils.assertEvaluatorExceptionES6(
                "identifier is a reserved word: await", "async function f() { void await; }");
    }

    @Test
    public void awaitAsIdentifierInTypeofExprError() {
        Utils.assertEvaluatorExceptionES6(
                "identifier is a reserved word: await", "async function f() { typeof await; }");
    }

    @Test
    public void awaitVarOutsideAsyncFunctionOk() {
        Utils.assertWithAllModes_ES6(1, "function f() { var await = 1; return await; } f()");
    }

    @Test
    public void awaitLabelOutsideAsyncFunctionOk() {
        Utils.assertWithAllModes_ES6(
                1, "function f() { await: { var x = 1; break await; } return x; } f()");
    }
}
