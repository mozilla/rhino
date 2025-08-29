/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mozilla.javascript.tests.es2025;

import org.junit.Test;
import org.mozilla.javascript.testutils.Utils;

/** Tests for ES2025 Math.f16round */
public class MathF16roundTest {

    @Test
    public void testBasicNumbers() {
        Utils.assertWithAllModes_ES6(1.0, "Math.f16round(1)");
        Utils.assertWithAllModes_ES6(-1.0, "Math.f16round(-1)");
        Utils.assertWithAllModes_ES6(2.0, "Math.f16round(2)");
        Utils.assertWithAllModes_ES6(0.5, "Math.f16round(0.5)");
    }

    @Test
    public void testZeros() {
        Utils.assertWithAllModes_ES6(0.0, "Math.f16round(0)");
        Utils.assertWithAllModes_ES6(-0.0, "Math.f16round(-0)");
        Utils.assertWithAllModes_ES6(0.0, "Math.f16round(0.0)");
        Utils.assertWithAllModes_ES6(-0.0, "Math.f16round(-0.0)");
    }

    @Test
    public void testSpecialValues() {
        Utils.assertWithAllModes_ES6(Double.NaN, "Math.f16round(NaN)");
        Utils.assertWithAllModes_ES6(Double.POSITIVE_INFINITY, "Math.f16round(Infinity)");
        Utils.assertWithAllModes_ES6(Double.NEGATIVE_INFINITY, "Math.f16round(-Infinity)");
    }

    @Test
    public void testMaxMinValues() {
        // Max finite half precision value: 65504
        Utils.assertWithAllModes_ES6(65504.0, "Math.f16round(65504)");
        Utils.assertWithAllModes_ES6(-65504.0, "Math.f16round(-65504)");

        // Values that overflow to infinity
        Utils.assertWithAllModes_ES6(Double.POSITIVE_INFINITY, "Math.f16round(65536)");
        Utils.assertWithAllModes_ES6(Double.NEGATIVE_INFINITY, "Math.f16round(-65536)");
    }

    @Test
    public void testSmallValues() {
        // Min positive normal: 2^-14 = 0.00006103515625
        final String script1 = "Math.f16round(0.00006103515625)";
        Utils.assertWithAllModes_ES6(0.00006103515625, script1);

        // Values that underflow to zero
        Utils.assertWithAllModes_ES6(0.0, "Math.f16round(1e-8)");
        Utils.assertWithAllModes_ES6(-0.0, "Math.f16round(-1e-8)");
    }

    @Test
    public void testRounding() {
        // Test basic rounding behavior
        Utils.assertWithAllModes_ES6(1.0, "Math.f16round(1.0)");
        Utils.assertWithAllModes_ES6(2.0, "Math.f16round(2.0)");
    }

    @Test
    public void testPrecisionLoss() {
        // Test that precision is lost appropriately
        final String script = "Math.f16round(Math.PI)";
        Utils.assertWithAllModes_ES6(3.140625, script);
    }

    @Test
    public void testPropertyDescriptor() {
        final String script =
                "var desc = Object.getOwnPropertyDescriptor(Math, 'f16round');"
                        + "desc.writable === true && "
                        + "desc.enumerable === false && "
                        + "desc.configurable === true && "
                        + "typeof desc.value === 'function';";
        Utils.assertWithAllModes_ES6(true, script);
    }

    @Test
    public void testFunctionProperties() {
        Utils.assertWithAllModes_ES6("f16round", "Math.f16round.name");
        Utils.assertWithAllModes_ES6(1.0, "Math.f16round.length");
    }

    @Test
    public void testNoArguments() {
        Utils.assertWithAllModes_ES6(Double.NaN, "Math.f16round()");
    }

    @Test
    public void testMultipleArguments() {
        Utils.assertWithAllModes_ES6(1.0, "Math.f16round(1, 2, 3)");
    }

    @Test
    public void testNonNumericArguments() {
        Utils.assertWithAllModes_ES6(Double.NaN, "Math.f16round('abc')");
        Utils.assertWithAllModes_ES6(Double.NaN, "Math.f16round(undefined)");
        Utils.assertWithAllModes_ES6(0.0, "Math.f16round(null)");
        Utils.assertWithAllModes_ES6(1.0, "Math.f16round(true)");
        Utils.assertWithAllModes_ES6(0.0, "Math.f16round(false)");
    }
}
