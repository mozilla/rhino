package org.mozilla.javascript.tests.backwardcompat;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.tests.Utils;

public class BackwardParseInt {

    @Test
    public void parseIntOctal_1_4() {
        Utils.assertWithAllOptimizationLevels(Context.VERSION_1_4, 7, "parseInt('07');");
        Utils.assertWithAllOptimizationLevels(Context.VERSION_1_4, 7, "parseInt('007');");
        Utils.assertWithAllOptimizationLevels(Context.VERSION_1_4, -56, "parseInt('-070');");
        Utils.assertWithAllOptimizationLevels(Context.VERSION_1_4, Double.NaN, "parseInt('08');");
        Utils.assertWithAllOptimizationLevels(Context.VERSION_1_4, 0, "parseInt('008');");
        Utils.assertWithAllOptimizationLevels(Context.VERSION_1_4, Double.NaN, "parseInt('-090');");
    }

    @Test
    public void parseIntOctal_1_5() {
        Utils.assertWithAllOptimizationLevels(Context.VERSION_1_5, 7, "parseInt('07');");
        Utils.assertWithAllOptimizationLevels(Context.VERSION_1_5, 7, "parseInt('007');");
        Utils.assertWithAllOptimizationLevels(Context.VERSION_1_5, -70, "parseInt('-070');");
        Utils.assertWithAllOptimizationLevels(Context.VERSION_1_5, 8, "parseInt('08');");
        Utils.assertWithAllOptimizationLevels(Context.VERSION_1_5, 8, "parseInt('008');");
        Utils.assertWithAllOptimizationLevels(Context.VERSION_1_5, -90, "parseInt('-090');");
    }

    @Test
    public void parseIntOctal_ES6() {
        Utils.assertWithAllOptimizationLevelsES6(7, "parseInt('07');");
        Utils.assertWithAllOptimizationLevelsES6(7, "parseInt('007');");
        Utils.assertWithAllOptimizationLevelsES6(-70, "parseInt('-070');");
        Utils.assertWithAllOptimizationLevelsES6(8, "parseInt('08');");
        Utils.assertWithAllOptimizationLevelsES6(8, "parseInt('008');");
        Utils.assertWithAllOptimizationLevelsES6(-90, "parseInt('-090');");
    }
}
