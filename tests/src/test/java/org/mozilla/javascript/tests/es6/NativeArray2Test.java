package org.mozilla.javascript.tests.es6;

import org.junit.Test;
import org.mozilla.javascript.tests.Utils;

/** Test for NativeArray. */
public class NativeArray2Test {

    @Test
    public void concatLimitSpreadable() {
        String js =
                "var spreadable = {};\n"
                        + "spreadable.length = Number.MAX_SAFE_INTEGER;\n"
                        + "spreadable[Symbol.isConcatSpreadable] = true;\n"
                        + "try {\n"
                        + "  [1].concat(spreadable);\n"
                        + "} catch(e) {"
                        + " '' + e;\n"
                        + "};";

        Utils.assertWithAllModes_ES6(
                "TypeError: Array length 9,007,199,254,740,992 exceeds supported capacity limit.",
                js);
    }

    @Test
    public void concatLimitSpreadable2() {
        String js =
                "var spreadable = {\n"
                        + "  length: Number.MAX_SAFE_INTEGER,\n"
                        + "  get 0() {\n"
                        + "    throw new Error('get failed');\n"
                        + "  },\n"
                        + "};\n"
                        + "spreadable[Symbol.isConcatSpreadable] = true;\n"
                        + "try {\n"
                        + "  [].concat(spreadable);\n"
                        + "} catch(e) {"
                        + " '' + e;\n"
                        + "};";

        Utils.assertWithAllModes_ES6("Error: get failed", js);
    }
}
