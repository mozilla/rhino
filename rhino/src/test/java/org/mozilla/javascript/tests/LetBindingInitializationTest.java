package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.testutils.Utils;

class LetBindingInitializationTest {

    @Test
    void topLevelLetIsNotAPropertyOfTheGlobalObject() {
        Utils.assertWithAllModes_ES6(
                "1,undefined,false",
                "let x = 1; [x, String(globalThis.x), 'x' in globalThis].join();");
    }

    @Test
    void topLevelLetShadowsGlobalObjectProperty() {
        Utils.runWithAllModes(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    var scope = cx.initStandardObjects();
                    cx.evaluateString(scope, "globalThis.y = 'global';", "first", 1, null);
                    Object result =
                            cx.evaluateString(
                                    scope,
                                    "let y = 'lexical'; [y, globalThis.y].join();",
                                    "second",
                                    1,
                                    null);
                    assertEquals("lexical,global", result);
                    assertEquals(
                            "lexical,global",
                            cx.evaluateString(
                                    scope, "[y, globalThis.y].join();", "third", 1, null));
                    return null;
                });
    }

    @Test
    void letWithoutInitializerIsUndefined() {
        Utils.assertWithAllModes_ES6(
                "undefined,undefined,undefined,undefined",
                """
                let a;
                function flattened() { let b; return b; }
                function reified() { let c; eval(''); return c; }
                var d;
                { let e; d = e; }
                [String(a), String(flattened()), String(reified()), String(d)].join();
                """);
    }

    @Test
    void letWithoutInitializerIsResetOnEachIteration() {
        Utils.assertWithAllModes_ES6(
                "undefined,undefined,undefined",
                """
                var o = [];
                for (var i = 0; i < 3; i++) {
                    let x;
                    o.push(String(x));
                    x = i;
                }
                o.join();
                """);
    }

    @Test
    void letDestructuringDefaults() {
        Utils.assertWithAllModes_ES6(
                "1,3,4,2|1,3,4,2|1,3,4,2",
                """
                function flattened() {
                    let [a = 1, b = 2] = [undefined, 3], {c = 4, d = 2} = {};
                    return [a, b, c, d].join();
                }
                function reified() {
                    let [a = 1, b = 2] = [undefined, 3], {c = 4, d = 2} = {};
                    eval('');
                    return [a, b, c, d].join();
                }
                let [a = 1, b = 2] = [undefined, 3], {c = 4, d = 2} = {};
                [flattened(), reified(), [a, b, c, d].join()].join('|');
                """);
    }

    @Test
    void forOfLetGivesEachIterationItsOwnBinding() {
        Utils.assertWithAllModes_ES6(
                "a,b,c|1,2",
                """
                var fs = [];
                for (let x of ['a', 'b', 'c']) fs.push(() => x);
                var gs = [];
                for (let [k, v] of [[0, 1], [0, 2]]) gs.push(() => v);
                [fs.map(f => f()).join(), gs.map(g => g()).join()].join('|');
                """);
    }

    @Test
    void forLetHeadIsInitialized() {
        Utils.assertWithAllModes_ES6(
                "0,1,undefined",
                """
                var fs = [];
                for (let i = 0, j; i < 2; i++) fs.push(() => i);
                var r;
                for (let i = 0, j; i < 1; i++) r = String(j);
                [fs[0](), fs[1](), r].join();
                """);
    }

    @Test
    void letInEvalCode() {
        Utils.assertWithAllModes_ES6(
                "5,7",
                "var r = eval('let z = 5; z'); function f() { return eval('let w = 7; w'); } [r, f()].join();");
    }
}
