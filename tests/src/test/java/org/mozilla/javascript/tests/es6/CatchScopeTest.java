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
}
