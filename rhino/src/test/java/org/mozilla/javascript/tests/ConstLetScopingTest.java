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

    @Test
    void varRedeclarationOfParamIsAllowed() {
        // var hoists to function scope, so redeclaring in a nested block must be fine
        Utils.assertWithAllModes_ES6(3.0, "(function f(a) { var a; return a; })(3)");
        Utils.assertWithAllModes_ES6(3.0, "(function f(a) { 'use strict'; var a; return a; })(3)");
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

    // --- block-scoped function declarations in strict mode ---

    @Test
    void blockScopedFunctionInStrictModeNotVisibleOutside() {
        // In strict mode, a function declared inside a block should not be visible outside it
        Utils.assertWithAllModes_ES6(
                "undefined", "'use strict'; { function f() { return 1; } } typeof f;");
    }

    @Test
    void blockScopedFunctionInStrictModeVisibleInside() {
        // In strict mode, a function declared inside a block should be callable within the block
        Utils.assertWithAllModes_ES6(
                1.0,
                "'use strict'; var result; { function f() { return 1; } result = f(); } result;");
    }

    @Test
    void topLevelFunctionInStrictModeStillHoists() {
        // Top-level function declarations in strict mode should still hoist normally
        Utils.assertWithAllModes_ES6(1.0, "'use strict'; function f() { return 1; } f();");
    }

    @Test
    void blockScopedFunctionShadowsOuterInStrictMode() {
        // A block-scoped function should shadow an outer function within the block
        Utils.assertWithAllModes_ES6(
                2.0,
                "'use strict'; function f() { return 1; } var result; { function f() { return 2; } result = f(); } result;");
    }

    @Test
    void outerFunctionUnchangedAfterBlockInStrictMode() {
        // The outer function should be unaffected after the block exits
        Utils.assertWithAllModes_ES6(
                1.0,
                "'use strict'; function f() { return 1; } { function f() { return 2; } } f();");
    }

    // --- re-entering a block scope inside a loop ---
    //
    // A function that needs no activation holds its block-scoped bindings in flat slots, one per
    // binding for the whole call. Every iteration must still see a fresh binding.

    @Test
    void constInForBodyIsRebound() {
        Utils.assertWithAllModes_ES6(
                "1,2,3",
                "function f(a) { var o = []; for (var i = 0; i < a.length; i++) {"
                        + " const x = a[i]; o.push(x); } return o.join(); } f([1, 2, 3]);");
    }

    @Test
    void constInWhileBodyIsRebound() {
        Utils.assertWithAllModes_ES6(
                "0,1,2",
                "function f(n) { var o = []; var i = 0; while (i < n) {"
                        + " const x = i; o.push(x); i++; } return o.join(); } f(3);");
    }

    @Test
    void constInDoWhileBodyIsRebound() {
        Utils.assertWithAllModes_ES6(
                "0,1,2",
                "function f(n) { var o = []; var i = 0; do {"
                        + " const x = i; o.push(x); i++; } while (i < n); return o.join(); } f(3);");
    }

    @Test
    void constInNestedBlocksIsRebound() {
        Utils.assertWithAllModes_ES6(
                "0:0,1:2,2:4",
                "function f(n) { var o = []; for (var i = 0; i < n; i++) { { const x = i;"
                        + " { const y = i * 2; o.push(x + ':' + y); } } } return o.join(); } f(3);");
    }

    @Test
    void constInForBodyIsReboundAfterContinue() {
        // The declaration is skipped on the middle iteration, and must still re-bind on the next
        Utils.assertWithAllModes_ES6(
                "0,skip,2",
                "function f(n) { var o = []; for (var i = 0; i < n; i++) {"
                        + " if (i === 1) { o.push('skip'); continue; } const x = i; o.push(x); }"
                        + " return o.join(); } f(3);");
    }

    @Test
    void letWithoutInitializerInForBodyIsReset() {
        // Reading the binding before its first assignment must see undefined on every iteration,
        // not the value the previous iteration left in the slot
        Utils.assertWithAllModes_ES6(
                "undefined,undefined,undefined",
                "function f(n) { var o = []; for (var i = 0; i < n; i++) {"
                        + " let x; o.push(String(x)); x = i; } return o.join(); } f(3);");
    }

    @Test
    void constDeclaredInSwitchCaseIsResetOnReentry() {
        // The declaration only runs for case 0, so on the next iteration the binding must be
        // undefined again rather than holding the value from the first pass
        Utils.assertWithAllModes_ES6(
                "7,undefined,d",
                "function f(n) { var o = []; for (var i = 0; i < n; i++) { switch (i) {"
                        + " case 0: const x = 7; o.push(x); break;"
                        + " case 1: o.push(String(x)); break;"
                        + " default: o.push('d'); } } return o.join(); } f(3);");
    }

    @Test
    void constForOfHeadIsBoundEachIteration() {
        Utils.assertWithAllModes_ES6(
                "1,2,3",
                "function f(a) { var o = []; for (const x of a) { o.push(x); }"
                        + " return o.join(); } f([1, 2, 3]);");
    }

    @Test
    void constForInHeadIsBoundEachIteration() {
        Utils.assertWithAllModes_ES6(
                "a,b",
                "function f(obj) { var o = []; for (const k in obj) { o.push(k); }"
                        + " return o.join(); } f({a: 1, b: 2});");
    }

    @Test
    void destructuringConstInForBodyIsRebound() {
        Utils.assertWithAllModes_ES6(
                "1/2,3/4",
                "function f(a) { var o = []; for (var i = 0; i < a.length; i++) {"
                        + " const [p, q] = a[i]; o.push(p + '/' + q); } return o.join(); }"
                        + " f([[1, 2], [3, 4]]);");
    }

    @Test
    void constInLoopStillRejectsAssignment() {
        Utils.assertWithAllModes_ES6(
                "protected",
                "function f() { for (var i = 0; i < 2; i++) { const x = i; x = 99;"
                        + " if (x !== i) return 'leaked'; } return 'protected'; } f();");
    }

    @Test
    void constInLoopIsReboundWithActivation() {
        // The same loop in a function that does reify its scopes, for comparison
        Utils.assertWithAllModes_ES6(
                "1,2,3",
                "function f(a) { var o = []; for (var i = 0; i < a.length; i++) {"
                        + " const x = a[i]; o.push(x); } eval(''); return o.join(); } f([1, 2, 3]);");
    }

    @Test
    void constInLoopStaysNumericallyCorrect() {
        // The declaration initializes unconditionally, so the slot can still be a number var
        Utils.assertWithAllModes_ES6(
                12.0,
                "function f(n) { var s = 0; for (var i = 0; i < n; i++) { const x = i * 2;"
                        + " s += x; } return s; } f(4);");
    }

    @Test
    void legacyConstInLoopStillAssignsOnce() {
        // Pre-ES6 const is hoisted to the function scope and keeps its "assign once" behaviour
        Utils.assertWithAllModes_1_8(
                "0,0,0",
                "function f(n) { var o = []; for (var i = 0; i < n; i++) { const x = i;"
                        + " o.push(x); } return o.join(); } f(3);");
    }
}
