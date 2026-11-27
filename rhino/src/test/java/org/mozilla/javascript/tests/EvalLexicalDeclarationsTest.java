package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

class EvalLexicalDeclarationsTest {

    private static final String ERROR_NAME =
            "function errorName(f) { try { f(); return 'none'; } catch (e) { return e.name; } }\n";

    @Test
    void directEvalLexicalsDoNotLeak() {
        Utils.assertWithAllModes_ES6(
                "undefined,undefined,undefined,undefined,number,function",
                """
                function inFunction() { eval('let a = 1; const b = 2;'); return [typeof a, typeof b]; }
                function inBlock() { { eval('let c = 1;'); } return typeof c; }
                eval('let d = 1;');
                function vars() { eval('var e = 1; function f() {}'); return [typeof e, typeof f]; }
                inFunction().concat([inBlock()], [typeof d], vars()).join();
                """);
    }

    @Test
    void indirectEvalLexicalsDoNotLeak() {
        Utils.assertWithAllModes_ES6(
                "undefined,undefined,false,true",
                """
                (0, eval)('let a = 1; const b = 2; var c = 3;');
                [typeof a, typeof b, 'a' in globalThis, 'c' in globalThis].join();
                """);
    }

    @Test
    void evalLexicalsAreVisibleInsideTheEval() {
        Utils.assertWithAllModes_ES6(
                "5,4,2",
                """
                var f = eval('let k = 3; var g = function() { return k; }; k = 4; g');
                [eval('let a = 5; a'), f(), eval("let n = 1; eval('n + 1')")].join();
                """);
    }

    @Test
    void evalLexicalsShadowOuterBindings() {
        Utils.assertWithAllModes_ES6(
                "1,1",
                """
                let g = 1;
                (0, eval)('let g = 2;');
                function f() { var j = 1; eval('let j = 2;'); return j; }
                [g, f()].join();
                """);
    }

    @Test
    void evalLetHasTemporalDeadZone() {
        Utils.assertWithAllModes_ES6(
                "ReferenceError,ReferenceError,ReferenceError",
                ERROR_NAME
                        + """
                [errorName(() => eval('x; let x = 1;')),
                 errorName(() => eval('typeof y; let y;')),
                 errorName(() => eval('var f = function() { return z; }; f(); let z;'))].join();
                """);
    }

    @Test
    void evalVarConflictingWithLexicalIsSyntaxError() {
        Utils.assertWithAllModes_ES6(
                "SyntaxError,SyntaxError,SyntaxError,SyntaxError,SyntaxError,SyntaxError",
                ERROR_NAME
                        + """
                let g = 1;
                [errorName(function() { let a = 1; eval('var a = 2;'); }),
                 errorName(function() { { let b = 1; eval('var b = 2;'); } }),
                 errorName(function() { { let c = 1; eval('function c() {}'); } }),
                 errorName(function() { eval("let d = 1; eval('var d = 2;');"); }),
                 errorName(function() { (0, eval)('var g;'); }),
                 errorName(function() { (0, eval)('function g() {}'); })].join();
                """);
    }

    @Test
    void failedEvalDeclaresNothing() {
        Utils.assertWithAllModes_ES6(
                "SyntaxError,undefined",
                ERROR_NAME
                        + """
                let g = 1;
                [errorName(function() { (0, eval)('var h; var g;'); }), typeof h].join();
                """);
    }

    @Test
    void evalVarMayRedeclareCatchParameterAndOuterVar() {
        Utils.assertWithAllModes_ES6(
                "none,none",
                ERROR_NAME
                        + """
                [errorName(function() { try { throw 1; } catch (e) { eval('var e = 2;'); } }),
                 errorName(function() { var v = 1; eval('var v = 2;'); })].join();
                """);
    }

    @Test
    void evalVarKeepsExistingValue() {
        Utils.assertWithAllModes_ES6(
                "123,123,123",
                """
                var r = [];
                (function(f) { eval('r.push(f); var f;'); })(123);
                (function() { var g = 123; eval('r.push(g); var g;'); })();
                var h = 123;
                eval('r.push(h); var h;');
                r.join();
                """);
    }

    @Test
    void annexBBlockFunctionInEvalKeepsExistingValue() {
        Utils.assertWithAllModes_ES6(
                "123,function,123,function",
                """
                var r = [];
                (function(f) {
                  eval('r.push(f); if (true) function f() {} else function _f() {} r.push(typeof f);');
                })(123);
                (function(f) {
                  eval('r.push(f); { function f() {} } r.push(typeof f);');
                })(123);
                r.join();
                """);
    }

    @Test
    void indirectEvalIsNeverStrict() {
        Utils.assertWithAllModes_ES6(
                "1",
                """
                'use strict';
                String((0, eval)('var static = 1; static'));
                """);
    }

    @Test
    void strictEvalVarsDoNotLeak() {
        Utils.assertWithAllModes_ES6(
                "undefined,undefined",
                """
                function f() { 'use strict'; eval('var a = 1; let b = 2;'); return [typeof a, typeof b]; }
                f().join();
                """);
    }

    @Test
    void pre6EvalLetStillLeaks() {
        Utils.assertWithAllModes_1_8(
                "number", "function f() { eval('let a = 1;'); return typeof a; } f();");
    }
}
