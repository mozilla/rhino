package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

class SwitchScopeTest {

    @Test
    void discriminantDoesNotSeeCaseLet() {
        Utils.assertWithAllModes_ES6(
                "outer",
                "let x = 'outer';\n"
                        + "var r;\n"
                        + "switch (x) { case 'outer': let x = 'inner'; r = 'outer'; break; }\n"
                        + "r");
    }

    @Test
    void discriminantDoesNotSeeCaseLetInFunction() {
        Utils.assertWithAllModes_ES6(
                "outer",
                "function f() {\n"
                        + "  let x = 'outer';\n"
                        + "  switch (x) { case 'outer': let x = 'inner'; return 'outer'; }\n"
                        + "  return 'none';\n"
                        + "}\n"
                        + "f()");
    }

    @Test
    void discriminantClosureCapturesOuterScope() {
        Utils.assertWithAllModes_ES6(
                "outer",
                "let x = 'outer';\n"
                        + "var g;\n"
                        + "switch (g = () => x) { default: let x = 'inner'; }\n"
                        + "g()");
    }

    @Test
    void discriminantClosureCapturesOuterScopeInFunction() {
        Utils.assertWithAllModes_ES6(
                "outer",
                "function f() {\n"
                        + "  let x = 'outer';\n"
                        + "  var g;\n"
                        + "  switch (g = () => x) { default: let x = 'inner'; }\n"
                        + "  return g();\n"
                        + "}\n"
                        + "f()");
    }

    @Test
    void caseExpressionSeesCaseLet() {
        Utils.assertWithAllModes_ES6(
                "inner",
                "let x = 1;\n"
                        + "var r;\n"
                        + "switch (undefined) { case x: r = 'inner'; break;"
                        + " default: r = 'outer'; let x = 2; }\n"
                        + "r");
    }

    @Test
    void discriminantEvaluatedOnce() {
        Utils.assertWithAllModes_ES6(
                "1:b",
                "var n = 0;\n"
                        + "var r;\n"
                        + "switch (++n) { case 0: r = 'a'; break; case 1: let y = 'b'; r = y;"
                        + " break; case 2: r = 'c'; }\n"
                        + "n + ':' + r");
    }

    @Test
    void discriminantInLoop() {
        Utils.assertWithAllModes_ES6(
                "abc",
                "var r = '';\n"
                        + "for (let i = 0; i < 3; i++) {\n"
                        + "  switch (i) { case 0: let a = 'a'; r += a; break;"
                        + " case 1: let b = 'b'; r += b; break; default: let c = 'c'; r += c; }\n"
                        + "}\n"
                        + "r");
    }

    @Test
    void discriminantWithYield() {
        Utils.assertWithAllModes_ES6(
                "bar",
                "function* g() {\n"
                        + "  switch (yield 1) { case 'foo': let x = 'bar'; return x; }\n"
                        + "}\n"
                        + "var it = g(); it.next(); it.next('foo').value");
    }
}
