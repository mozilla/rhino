package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

class CatchScopeTest {

    @Test
    void catchBlockWithLetLeavesCatchScope() {
        Utils.assertWithAllModes_ES6(
                "0,1,2",
                "var r = [];\n"
                        + "for (let i = 0; i < 3; i++) {\n"
                        + "  try { throw i; } catch (e) { let x = e; r.push(x); }\n"
                        + "}\n"
                        + "r.join()");
    }

    @Test
    void catchBlockWithLetLeavesCatchScopeInFunction() {
        Utils.assertWithAllModes_ES6(
                "undefined",
                "function f() {\n"
                        + "  try { throw 1; } catch (e) { const x = e; }\n"
                        + "  return typeof e;\n"
                        + "}\n"
                        + "f()");
    }

    @Test
    void catchBlockClosureCapturesLet() {
        Utils.assertWithAllModes_ES6(
                "0,1",
                "var r = [];\n"
                        + "for (let i = 0; i < 2; i++) {\n"
                        + "  try { throw i; } catch (e) { let x = e; r.push(() => x); }\n"
                        + "}\n"
                        + "r.map(g => g()).join()");
    }

    @Test
    void continueFromCatchLeavesCatchScope() {
        Utils.assertWithAllModes_ES6(
                "0,1,2",
                "var r = [];\n"
                        + "for (let i = 0; i < 3; i++) {\n"
                        + "  try { throw i; } catch (e) { r.push(e); continue; }\n"
                        + "}\n"
                        + "r.join()");
    }

    @Test
    void continueFromCatchWithLetLeavesCatchScope() {
        Utils.assertWithAllModes_ES6(
                "0,1,2",
                "var r = [];\n"
                        + "for (let i = 0; i < 3; i++) {\n"
                        + "  try { throw i; } catch (e) { let x = e; r.push(() => x); continue; }\n"
                        + "}\n"
                        + "r.map(g => g()).join()");
    }

    @Test
    void labelledContinueFromNestedCatchLeavesCatchScopes() {
        Utils.assertWithAllModes_ES6(
                "0,1,undefined",
                "var r = [];\n"
                        + "outer: for (let i = 0; i < 2; i++) {\n"
                        + "  for (let j = 0; j < 2; j++) {\n"
                        + "    try { throw i; } catch (e) {\n"
                        + "      try { throw j; } catch (e2) { r.push(e); continue outer; }\n"
                        + "    }\n"
                        + "  }\n"
                        + "}\n"
                        + "r.push(typeof e2);\n"
                        + "r.join()");
    }

    @Test
    void breakFromCatchLeavesCatchScope() {
        Utils.assertWithAllModes_ES6(
                "undefined",
                "for (let i = 0; i < 3; i++) {\n"
                        + "  try { throw i; } catch (e) { break; }\n"
                        + "}\n"
                        + "typeof e");
    }

    @Test
    void returnFromCatchThroughFinallyLeavesCatchScope() {
        Utils.assertWithAllModes_ES6(
                "1,undefined",
                "var r = [];\n"
                        + "function f() {\n"
                        + "  try {\n"
                        + "    try { throw 1; } catch (e) { return e; }\n"
                        + "  } finally { r.push(typeof e); }\n"
                        + "}\n"
                        + "r.unshift(f());\n"
                        + "r.join()");
    }
}
