package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * Temporal dead zone checks for lexical bindings, both held in scope objects (at the top level, in
 * blocks, and in functions that need an activation) and in a function's local variable slots.
 */
class LetTdzTest {

    private static final String TDZ_X = "ReferenceError: Cannot access \"x\" before initialization";

    @Test
    void readBeforeDeclarationInBlock() {
        Utils.assertEcmaErrorES6(TDZ_X, "{ x; let x = 1; }");
    }

    @Test
    void readBeforeDeclarationAtTopLevel() {
        Utils.assertEcmaErrorES6(TDZ_X, "x; let x = 1;");
    }

    @Test
    void readOfConstBeforeDeclaration() {
        Utils.assertEcmaErrorES6(TDZ_X, "{ x; const x = 1; }");
    }

    @Test
    void readInOwnInitializer() {
        Utils.assertEcmaErrorES6(TDZ_X, "{ let x = x; }");
    }

    @Test
    void readInDestructuringDefault() {
        Utils.assertEcmaErrorES6(
                "ReferenceError: Cannot access \"b\" before initialization",
                "{ let [a = b, b] = []; }");
    }

    @Test
    void typeofBeforeDeclaration() {
        Utils.assertEcmaErrorES6(TDZ_X, "{ typeof x; let x; }");
    }

    @Test
    void typeofUndeclaredNameIsStillUndefined() {
        Utils.assertWithAllModes_ES6("undefined", "{ let x; } typeof x;");
    }

    @Test
    void assignmentBeforeDeclaration() {
        Utils.assertEcmaErrorES6(TDZ_X, "{ x = 1; let x; }");
    }

    @Test
    void strictAssignmentBeforeDeclaration() {
        Utils.assertEcmaErrorES6(TDZ_X, "'use strict'; { x = 1; let x; }");
    }

    @Test
    void assignmentToConstBeforeDeclaration() {
        Utils.assertEcmaErrorES6(TDZ_X, "{ x = 1; const x = 2; }");
    }

    @Test
    void incrementBeforeDeclaration() {
        Utils.assertEcmaErrorES6(TDZ_X, "{ x++; let x = 1; }");
    }

    @Test
    void compoundAssignmentBeforeDeclaration() {
        Utils.assertEcmaErrorES6(TDZ_X, "{ x += 1; let x = 1; }");
    }

    @Test
    void callBeforeDeclaration() {
        Utils.assertEcmaErrorES6(TDZ_X, "{ x(); let x = function() {}; }");
    }

    @Test
    void closureCalledBeforeDeclaration() {
        Utils.assertEcmaErrorES6(
                TDZ_X, "function f() { function g() { return x; } g(); let x = 1; } f();");
    }

    @Test
    void closureCalledAfterDeclaration() {
        Utils.assertWithAllModes_ES6(
                1, "function f() { function g() { return x; } let x = 1; return g(); } f();");
    }

    @Test
    void switchCaseJumpingPastDeclaration() {
        Utils.assertEcmaErrorES6(TDZ_X, "switch (1) { case 0: let x = 1; case 1: x; }");
    }

    @Test
    void eachLoopIterationHasItsOwnDeadZone() {
        Utils.assertWithAllModes_ES6(
                "0:ReferenceError,1:ReferenceError",
                """
                var o = [];
                for (var i = 0; i < 2; i++) {
                    let g = function() { return x; };
                    try { g(); } catch (e) { o.push(i + ':' + e.name); }
                    let x = i;
                }
                o.join();
                """);
    }

    @Test
    void errorIsACatchableReferenceError() {
        Utils.assertWithAllModes_ES6(
                true, "try { { x; let x; } } catch (e) { e instanceof ReferenceError }");
    }

    @Test
    void globalObjectPropertyIsNotInDeadZone() {
        Utils.assertWithAllModes_ES6("undefined", "var r = String(globalThis.x); let x = 1; r;");
    }

    @Test
    void legacyLetHasNoDeadZone() {
        Utils.assertWithAllModes_1_8("undefined", "var r; { r = String(x); let x = 1; } r;");
    }

    @Test
    void legacyConstReadsUndefinedBeforeInitialization() {
        Utils.assertWithAllModes_1_8(
                "undefined,1,undefined",
                """
                var a = typeof q;
                const q = 1;
                function g() { var r = typeof z; const z = 2; eval(''); return r; }
                [a, q, g()].join();
                """);
    }

    // The functions below need no activation, so their bindings are held in local variable slots.

    @Test
    void localReadBeforeDeclaration() {
        Utils.assertEcmaErrorES6(TDZ_X, "function f() { var r = x; let x = 1; return r; } f();");
    }

    @Test
    void localConstReadBeforeDeclaration() {
        Utils.assertEcmaErrorES6(TDZ_X, "function f() { var r = x; const x = 1; return r; } f();");
    }

    @Test
    void localNumericConstReadBeforeDeclaration() {
        Utils.assertEcmaErrorES6(
                TDZ_X, "function f() { var r = 0; r = x + 1; const x = 1; return r; } f();");
    }

    @Test
    void localTypeofBeforeDeclaration() {
        Utils.assertEcmaErrorES6(TDZ_X, "function f() { typeof x; let x; } f();");
    }

    @Test
    void localAssignmentBeforeDeclaration() {
        Utils.assertEcmaErrorES6(TDZ_X, "function f() { x = 2; let x = 1; } f();");
    }

    @Test
    void localAssignmentToConstBeforeDeclaration() {
        Utils.assertEcmaErrorES6(TDZ_X, "function f() { x = 2; const x = 1; } f();");
    }

    @Test
    void localIncrementBeforeDeclaration() {
        Utils.assertEcmaErrorES6(TDZ_X, "function f() { x++; let x = 1; } f();");
    }

    @Test
    void localReadInOwnInitializer() {
        Utils.assertEcmaErrorES6(TDZ_X, "function f() { let x = x + 1; } f();");
    }

    @Test
    void localSwitchCaseJumpingPastDeclaration() {
        Utils.assertEcmaErrorES6(
                TDZ_X, "function f() { switch (1) { case 0: let x = 1; case 1: return x; } } f();");
    }

    @Test
    void localDeadZoneOnEachLoopIteration() {
        Utils.assertWithAllModes_ES6(
                "ReferenceError,ReferenceError",
                """
                function f() {
                    var o = [];
                    for (var i = 0; i < 3; i++) {
                        try { if (i > 0) o.push(x); } catch (e) { o.push(e.name); }
                        let x = i;
                    }
                    return o.join();
                }
                f();
                """);
    }

    @Test
    void localDeadZoneOnEachLoopIterationAfterEarlierDeclarator() {
        Utils.assertWithAllModes_ES6(
                "0:none,1:ReferenceError",
                """
                function f() {
                    var o = [];
                    for (var i = 0; i < 2; i++) {
                        try {
                            let a = i > 0 ? b : 'none', b = i;
                            o.push(i + ':' + a);
                        } catch (e) { o.push(i + ':' + e.name); }
                    }
                    return o.join();
                }
                f();
                """);
    }

    @Test
    void localStoreInSkippedBranchDoesNotInitialize() {
        Utils.assertEcmaErrorES6(
                TDZ_X, "function f() { if (false) x = 1; return x; let x = 2; } f();");
    }

    @Test
    void localStoreInSkippedOperandDoesNotInitialize() {
        Utils.assertEcmaErrorES6(
                TDZ_X, "function f() { false && (x = 1); return x; let x = 2; } f();");
        Utils.assertEcmaErrorES6(
                TDZ_X, "function f() { var o = null; o?.p(x = 1); return x; let x = 2; } f();");
    }

    @Test
    void localStoreInSkippedLoopDoesNotInitialize() {
        Utils.assertEcmaErrorES6(
                TDZ_X, "function f() { while (false) x = 1; return x; let x = 2; } f();");
    }

    @Test
    void localStoreAfterThrowDoesNotInitialize() {
        Utils.assertEcmaErrorES6(
                TDZ_X,
                "function f() { try { throw 0; x = 1; } catch (e) {} return x; let x = 2; } f();");
    }

    @Test
    void localLetWithoutInitializerIsFreshOnEachIteration() {
        Utils.assertWithAllModes_ES6(
                "undefined,undefined,undefined",
                """
                function f() {
                    var o = [];
                    for (var i = 0; i < 3; i++) {
                        let x;
                        o.push(String(x));
                        x = i;
                    }
                    return o.join();
                }
                f();
                """);
    }

    @Test
    void localBindingsWorkOnceInitialized() {
        Utils.assertWithAllModes_ES6(
                "3,6,undefined",
                """
                function f() {
                    let s = 0;
                    for (let i = 0; i < 3; i++) s += i;
                    const c = s * 2;
                    let u;
                    return [s, c, String(u)].join();
                }
                f();
                """);
    }
}
