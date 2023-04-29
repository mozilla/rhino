package org.mozilla.javascript.tests.backwardcompat;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.tests.Utils;
import org.mozilla.javascript.tools.shell.Global;

public class BackwardParseInt {

    @Test
    public void parseIntOctal_1_4() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_1_4);

                    Global root = new Global(cx);
                    Object obj = cx.evaluateString(root, "parseInt('07');", "[test]", 1, null);
                    assertEquals(7.0, obj);

                    obj = cx.evaluateString(root, "parseInt('007');", "[test]", 1, null);
                    assertEquals(7.0, obj);

                    obj = cx.evaluateString(root, "parseInt('-070');", "[test]", 1, null);
                    assertEquals(-56.0, obj);

                    obj = cx.evaluateString(root, "parseInt('08');", "[test]", 1, null);
                    assertEquals(Double.NaN, obj);

                    obj = cx.evaluateString(root, "parseInt('008');", "[test]", 1, null);
                    assertEquals(0.0, obj);

                    obj = cx.evaluateString(root, "parseInt('-090');", "[test]", 1, null);
                    assertEquals(Double.NaN, obj);

                    return null;
                });
    }

    @Test
    public void parseIntOctal_1_5() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_1_5);

                    Global root = new Global(cx);
                    Object obj = cx.evaluateString(root, "parseInt('07');", "[test]", 1, null);
                    assertEquals(7.0, obj);

                    obj = cx.evaluateString(root, "parseInt('007');", "[test]", 1, null);
                    assertEquals(7.0, obj);

                    obj = cx.evaluateString(root, "parseInt('-070');", "[test]", 1, null);
                    assertEquals(-70.0, obj);

                    obj = cx.evaluateString(root, "parseInt('08');", "[test]", 1, null);
                    assertEquals(8.0, obj);

                    obj = cx.evaluateString(root, "parseInt('008');", "[test]", 1, null);
                    assertEquals(8.0, obj);

                    obj = cx.evaluateString(root, "parseInt('-090');", "[test]", 1, null);
                    assertEquals(-90.0, obj);

                    return null;
                });
    }

    @Test
    public void parseIntOctal_ES6() {
        Utils.runWithAllOptimizationLevels(
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);

                    Global root = new Global(cx);
                    Object obj = cx.evaluateString(root, "parseInt('07');", "[test]", 1, null);
                    assertEquals(7.0, obj);

                    obj = cx.evaluateString(root, "parseInt('007');", "[test]", 1, null);
                    assertEquals(7.0, obj);

                    obj = cx.evaluateString(root, "parseInt('-070');", "[test]", 1, null);
                    assertEquals(-70.0, obj);

                    obj = cx.evaluateString(root, "parseInt('08');", "[test]", 1, null);
                    assertEquals(8.0, obj);

                    obj = cx.evaluateString(root, "parseInt('008');", "[test]", 1, null);
                    assertEquals(8.0, obj);

                    obj = cx.evaluateString(root, "parseInt('-090');", "[test]", 1, null);
                    assertEquals(-90.0, obj);

                    return null;
                });
    }
}
