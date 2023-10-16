package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

/** Test for NativeArray. */
public class NativeArray2Test {

    private Context cx;
    private ScriptableObject scope;

    @Before
    public void setUp() {
        cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
        scope = cx.initStandardObjects();
    }

    @After
    public void tearDown() {
        Context.exit();
    }

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

        String result = (String) cx.evaluateString(scope, js, "test", 1, null);
        assertTrue(result.endsWith("exceeds supported capacity limit."));
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

        String result = (String) cx.evaluateString(scope, js, "test", 1, null);
        assertEquals(result, "Error: get failed", result);
    }
}
