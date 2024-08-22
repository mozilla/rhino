package org.mozilla.javascript.tests.es6;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tests.Utils;

/** Test for TypedArrays. */
public class TypedArrayJavaTest {

    /**
     * Test case for {@link https://github.com/mozilla/rhino/issues/768}
     *
     * @throws Exception if test failed
     */
    @Test
    public void subarrayWithoutParams() throws Exception {
        String script = "var ta = new §§type§§([1, 2]);\n" + "'' + ta.subarray();";

        allTypes(script, "1,2");
    }

    /** @throws Exception if test failed */
    @Test
    public void subarrayFromSubarray() throws Exception {
        subarrayFromSubarray(0, 7, 0, 6, "1,2,3,4,5,6,7 - 1,2,3,4,5,6");
        subarrayFromSubarray(0, 7, 0, 7, "1,2,3,4,5,6,7 - 1,2,3,4,5,6,7");
        subarrayFromSubarray(0, 7, 0, 8, "1,2,3,4,5,6,7 - 1,2,3,4,5,6,7");
        subarrayFromSubarray(0, 7, 0, 80, "1,2,3,4,5,6,7 - 1,2,3,4,5,6,7");

        subarrayFromSubarray(1, 7, 0, 5, "2,3,4,5,6,7 - 2,3,4,5,6");
        subarrayFromSubarray(1, 7, 0, 6, "2,3,4,5,6,7 - 2,3,4,5,6,7");
        subarrayFromSubarray(1, 7, 0, 7, "2,3,4,5,6,7 - 2,3,4,5,6,7");
        subarrayFromSubarray(1, 7, 0, 70, "2,3,4,5,6,7 - 2,3,4,5,6,7");

        subarrayFromSubarray(0, 8, 0, 6, "1,2,3,4,5,6,7 - 1,2,3,4,5,6");
        subarrayFromSubarray(0, 8, 0, 7, "1,2,3,4,5,6,7 - 1,2,3,4,5,6,7");
        subarrayFromSubarray(0, 8, 0, 8, "1,2,3,4,5,6,7 - 1,2,3,4,5,6,7");
        subarrayFromSubarray(0, 8, 0, 80, "1,2,3,4,5,6,7 - 1,2,3,4,5,6,7");

        subarrayFromSubarray(1, 8, 0, 5, "2,3,4,5,6,7 - 2,3,4,5,6");
        subarrayFromSubarray(1, 8, 0, 6, "2,3,4,5,6,7 - 2,3,4,5,6,7");
        subarrayFromSubarray(1, 8, 0, 7, "2,3,4,5,6,7 - 2,3,4,5,6,7");
        subarrayFromSubarray(1, 8, 0, 70, "2,3,4,5,6,7 - 2,3,4,5,6,7");

        subarrayFromSubarray(0, 6, 0, 5, "1,2,3,4,5,6 - 1,2,3,4,5");
        subarrayFromSubarray(0, 6, 0, 6, "1,2,3,4,5,6 - 1,2,3,4,5,6");
        subarrayFromSubarray(0, 6, 0, 7, "1,2,3,4,5,6 - 1,2,3,4,5,6");
        subarrayFromSubarray(0, 6, 0, 70, "1,2,3,4,5,6 - 1,2,3,4,5,6");

        subarrayFromSubarray(2, 5, 0, 2, "3,4,5 - 3,4");
        subarrayFromSubarray(2, 5, 0, 3, "3,4,5 - 3,4,5");
        subarrayFromSubarray(2, 5, 0, 4, "3,4,5 - 3,4,5");
        subarrayFromSubarray(2, 5, 0, 40, "3,4,5 - 3,4,5");

        subarrayFromSubarray(2, 5, 1, 2, "3,4,5 - 4");
        subarrayFromSubarray(2, 5, 1, 3, "3,4,5 - 4,5");
        subarrayFromSubarray(2, 5, 1, 4, "3,4,5 - 4,5");
        subarrayFromSubarray(2, 5, 1, 40, "3,4,5 - 4,5");

        subarrayFromSubarray(2, 5, -1, Integer.MIN_VALUE, "3,4,5 - 5");
        subarrayFromSubarray(2, 5, -3, Integer.MIN_VALUE, "3,4,5 - 3,4,5");
        subarrayFromSubarray(2, 5, -4, Integer.MIN_VALUE, "3,4,5 - 3,4,5");
        subarrayFromSubarray(2, 5, -40, Integer.MIN_VALUE, "3,4,5 - 3,4,5");

        subarrayFromSubarray(-6, Integer.MIN_VALUE, 0, 5, "2,3,4,5,6,7 - 2,3,4,5,6");
        subarrayFromSubarray(-6, Integer.MIN_VALUE, 0, 6, "2,3,4,5,6,7 - 2,3,4,5,6,7");
        subarrayFromSubarray(-6, Integer.MIN_VALUE, 0, 7, "2,3,4,5,6,7 - 2,3,4,5,6,7");
        subarrayFromSubarray(-6, Integer.MIN_VALUE, 0, 70, "2,3,4,5,6,7 - 2,3,4,5,6,7");

        subarrayFromSubarray(2, 5, 0, -1, "3,4,5 - 3,4");
        subarrayFromSubarray(2, 5, 0, -2, "3,4,5 - 3");
        subarrayFromSubarray(2, 5, 0, -3, "3,4,5 - ");
        subarrayFromSubarray(2, 5, 0, -4, "3,4,5 - ");

        subarrayFromSubarray(2, 5, 1, -1, "3,4,5 - 4");
        subarrayFromSubarray(2, 5, 1, -2, "3,4,5 - ");
        subarrayFromSubarray(2, 5, 1, -3, "3,4,5 - ");
    }

    private static void subarrayFromSubarray(int from, int to, int from2, int to2, String expected)
            throws Exception {
        String script =
                "var arr = new §§type§§([1, 2, 3, 4, 5, 6, 7]);\n"
                        + "var sub1 = arr.subarray("
                        + from
                        + (Integer.MIN_VALUE != to ? (", " + to) : "")
                        + ");\n"
                        + "var sub2 = sub1.subarray("
                        + from2
                        + (Integer.MIN_VALUE != to2 ? (", " + to2) : "")
                        + ");\n"
                        + "'' + sub1 + ' - ' + sub2";

        allTypes(script, expected);
    }

    private static void allTypes(String script, String expected) throws Exception {
        String[] allNativeTypes = {
            "Float32Array",
            "Float64Array",
            "Int8Array",
            "Int16Array",
            "Int32Array",
            "Uint8Array",
            "Uint16Array",
            "Uint32Array",
            "Uint8ClampedArray"
        };

        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    ScriptableObject scope = cx.initStandardObjects();

                    for (String type : allNativeTypes) {
                        String scr = script.replace("§§type§§", type);
                        Object obj = cx.evaluateString(scope, scr, "", 1, null);
                        assertEquals(expected, obj);
                    }

                    return null;
                });
    }
}
