package org.mozilla.javascript.tests.backwardcompat;

import org.junit.Test;
import org.mozilla.javascript.tests.Utils;

public class BackwardParseInt {

    @Test
    public void parseIntOctal_1_4() {
        Utils.assertWithAllModes_1_4(7, "parseInt('07');");
        Utils.assertWithAllModes_1_4(7, "parseInt('007');");
        Utils.assertWithAllModes_1_4(-56, "parseInt('-070');");
        Utils.assertWithAllModes_1_4(Double.NaN, "parseInt('08');");
        Utils.assertWithAllModes_1_4(0, "parseInt('008');");
        Utils.assertWithAllModes_1_4(Double.NaN, "parseInt('-090');");
    }

    @Test
    public void parseIntOctal_1_5() {
        Utils.assertWithAllModes_1_5(7, "parseInt('07');");
        Utils.assertWithAllModes_1_5(7, "parseInt('007');");
        Utils.assertWithAllModes_1_5(-70, "parseInt('-070');");
        Utils.assertWithAllModes_1_5(8, "parseInt('08');");
        Utils.assertWithAllModes_1_5(8, "parseInt('008');");
        Utils.assertWithAllModes_1_5(-90, "parseInt('-090');");
    }

    @Test
    public void parseIntOctal_ES6() {
        Utils.assertWithAllModes_ES6(7, "parseInt('07');");
        Utils.assertWithAllModes_ES6(7, "parseInt('007');");
        Utils.assertWithAllModes_ES6(-70, "parseInt('-070');");
        Utils.assertWithAllModes_ES6(8, "parseInt('08');");
        Utils.assertWithAllModes_ES6(8, "parseInt('008');");
        Utils.assertWithAllModes_ES6(-90, "parseInt('-090');");
    }
}
