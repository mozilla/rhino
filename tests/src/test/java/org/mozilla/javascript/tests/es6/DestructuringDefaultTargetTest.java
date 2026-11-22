package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/**
 * A destructuring default is applied according to the value being destructured, whatever the target
 * held before.
 */
class DestructuringDefaultTargetTest {

    @Test
    void varDeclarationReplacesEarlierValue() {
        Utils.assertWithAllModes_ES6(
                "1,2", "var a = 5; var [a = 1] = []; var b = 5; var {b = 2} = {}; [a, b].join();");
    }

    @Test
    void localVarDeclarationReplacesEarlierValue() {
        Utils.assertWithAllModes_ES6(
                "1,2",
                """
                function f() {
                    var a = 5; var [a = 1] = [];
                    var b = 5; var {b = 2} = {};
                    return [a, b].join();
                }
                f();
                """);
    }

    @Test
    void assignmentReplacesEarlierValue() {
        Utils.assertWithAllModes_ES6(
                "1,2,3",
                """
                let a = 5, b = 5, c = 5;
                [a = 1] = [];
                ({b = 2} = {});
                [{c: c = 3}] = [{}];
                [a, b, c].join();
                """);
    }

    @Test
    void parameterDefaults() {
        Utils.assertWithAllModes_ES6(
                "1,2,1|4,3",
                """
                function f([a = 1] = []) { return a; }
                function g({a = 4} = {}, [b = 3] = []) { return a + ',' + b; }
                [f(), f([2]), f([undefined])].join() + '|' + g();
                """);
    }

    @Test
    void defaultIsNotEvaluatedForDefinedValue() {
        Utils.assertWithAllModes_ES6(
                "7,0",
                """
                var calls = 0;
                function d() { calls++; return 1; }
                var a;
                [a = d()] = [7];
                [a, calls].join();
                """);
    }

    @Test
    void assignmentToBindingInDeadZoneThrows() {
        Utils.assertEcmaErrorES6(
                "ReferenceError: Cannot access \"a\" before initialization",
                "{ [a = 1] = []; let a; }");
    }
}
