/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

public class MultiplyNegativeZeroTest {

    @Test
    public void zeroTimesNegative() {
        Utils.assertWithAllModes_ES6(true, "var x = 0, y = -1; Object.is(x * y, -0)");
    }

    @Test
    public void negativeTimesZero() {
        Utils.assertWithAllModes_ES6(true, "var x = -5, y = 0; Object.is(x * y, -0)");
    }

    @Test
    public void zeroTimesNegativeInFunction() {
        Utils.assertWithAllModes_ES6(
                -Double.POSITIVE_INFINITY, "function f(a, b) { return a * b; } 1 / f(0, -1)");
    }

    @Test
    public void zeroTimesPositive() {
        Utils.assertWithAllModes_ES6(true, "var x = 0, y = 3; Object.is(x * y, 0)");
    }

    @Test
    public void zeroTimesZero() {
        Utils.assertWithAllModes_ES6(true, "var x = 0, y = 0; Object.is(x * y, 0)");
    }
}
