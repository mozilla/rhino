package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

class GeneratorFunctionPrototypeTest {
    private static final String CHECK_PROTOTYPE =
            "function* g() {}\n"
                    + "var p = Object.getPrototypeOf(g);\n"
                    + "[Object.prototype.toString.call(p),\n"
                    + " Object.getPrototypeOf(p) === Function.prototype,\n"
                    + " g instanceof Function].join()";

    @Test
    void prototypeChain() {
        Utils.assertWithAllModes_ES6("[object GeneratorFunction],true,true", CHECK_PROTOTYPE);
    }

    @Test
    void prototypeChainWithTopLevelScope() {
        Utils.assertWithAllModesTopLevelScope_ES6(
                "[object GeneratorFunction],true,true", CHECK_PROTOTYPE);
    }

    @Test
    void callApplyAndBind() {
        Utils.assertWithAllModes_ES6(
                "1,2,3",
                "function* g(a) { yield this.v + a; }\n"
                        + "[g.call({v: 0}, 1).next().value,\n"
                        + " g.apply({v: 1}, [1]).next().value,\n"
                        + " g.bind({v: 2}, 1)().next().value].join()");
    }
}
