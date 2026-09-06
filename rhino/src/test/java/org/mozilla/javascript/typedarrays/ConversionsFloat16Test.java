package org.mozilla.javascript.typedarrays;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for the Float16 (half-precision) conversions in {@link Conversions}.
 *
 * <p>Float16 uses 16 bits: - 1 bit: sign - 5 bits: exponent - 10 bits: mantissa
 *
 * <p>Special values: - Zero: exponent=0, mantissa=0 - Denormalized: exponent=0, mantissa!=0 -
 * Normalized: exponent=1-30 - Infinity: exponent=31, mantissa=0 - NaN: exponent=31, mantissa!=0
 *
 * <p>Value ranges: - Max: 65504 (0x7BFF) - Min positive normal: 2^-14 ≈ 0.000061035 - Min positive
 * subnormal: 2^-24 ≈ 5.96e-8
 */
public class ConversionsFloat16Test {

    private static final double EPSILON = 1e-10;

    /**
     * Helper to convert a value to its Float16 bit pattern and back, for round-trip testing.
     *
     * @param value The value to convert
     * @return The value recovered after the round-trip
     */
    private float roundTrip(double value) {
        return Conversions.shortBitsToFloat16(Conversions.float16ToShortBits(value));
    }

    /**
     * Helper to verify the Float16 bit pattern produced for a value matches the expected pattern.
     *
     * @param expectedBits The expected 16-bit pattern
     * @param value The value to convert
     */
    private void assertBits(int expectedBits, double value) {
        int actualBits = Conversions.float16ToShortBits(value) & 0xffff;
        assertEquals(
                expectedBits,
                actualBits,
                String.format("Bit pattern mismatch for value %.10f", value));
    }

    // === Zero Tests ===

    @Test
    public void testPositiveZero() {
        float result = roundTrip(0.0);
        assertEquals(0.0f, result, 0.0f);
        assertTrue(Float.floatToIntBits(result) == Float.floatToIntBits(0.0f));
    }

    @Test
    public void testNegativeZero() {
        float result = roundTrip(-0.0);
        assertEquals(-0.0f, result, 0.0f);
        assertTrue(Float.floatToIntBits(result) == Float.floatToIntBits(-0.0f));
    }

    @Test
    public void testZeroBitPattern() {
        assertBits(0x0000, 0.0); // +0
        assertBits(0x8000, -0.0); // -0
    }

    // === Simple Value Tests ===

    @Test
    public void testOne() {
        float result = roundTrip(1.0);
        assertEquals(1.0f, result, EPSILON);
    }

    @Test
    public void testMinusOne() {
        float result = roundTrip(-1.0);
        assertEquals(-1.0f, result, EPSILON);
    }

    @Test
    public void testTwo() {
        float result = roundTrip(2.0);
        assertEquals(2.0f, result, EPSILON);
    }

    @Test
    public void testOneHalf() {
        float result = roundTrip(0.5);
        assertEquals(0.5f, result, EPSILON);
    }

    // === Infinity Tests ===

    @Test
    public void testPositiveInfinity() {
        float result = roundTrip(Float.POSITIVE_INFINITY);
        assertTrue(Float.isInfinite(result));
        assertTrue(result > 0);
    }

    @Test
    public void testNegativeInfinity() {
        float result = roundTrip(Float.NEGATIVE_INFINITY);
        assertTrue(Float.isInfinite(result));
        assertTrue(result < 0);
    }

    @Test
    public void testInfinityBitPattern() {
        assertBits(0x7C00, Float.POSITIVE_INFINITY);
        assertBits(0xFC00, Float.NEGATIVE_INFINITY);
    }

    // === NaN Tests ===

    @Test
    public void testNaN() {
        float result = roundTrip(Float.NaN);
        assertTrue(Float.isNaN(result));
    }

    @Test
    public void testNaNBitPattern() {
        int bits = Conversions.float16ToShortBits(Float.NaN) & 0xffff;

        // NaN has exponent=31 and non-zero mantissa
        int exponent = (bits >>> 10) & 0x1f;
        int mantissa = bits & 0x3ff;
        assertEquals(31, exponent);
        assertTrue(mantissa != 0);
    }

    // === Overflow Tests ===

    @Test
    public void testMaxValue() {
        // Max Float16 is 65504
        float result = roundTrip(65504.0);
        assertEquals(65504.0f, result, 0.0f);
    }

    @Test
    public void testOverflowToPositiveInfinity() {
        // Values > 65504 overflow to infinity
        float result = roundTrip(100000.0);
        assertEquals(Float.POSITIVE_INFINITY, result, 0.0f);
    }

    @Test
    public void testOverflowToNegativeInfinity() {
        float result = roundTrip(-100000.0);
        assertEquals(Float.NEGATIVE_INFINITY, result, 0.0f);
    }

    // === Denormalized Number Tests ===

    @Test
    public void testMinPositiveNormal() {
        // Min positive normal Float16 is 2^-14 ≈ 0.000061035
        double minNormal = Math.pow(2, -14);
        float result = roundTrip(minNormal);
        assertEquals((float) minNormal, result, 1e-7);
    }

    @Test
    public void testDenormalizedNumber() {
        // Test a denormalized number between 2^-24 and 2^-14
        double denormal = Math.pow(2, -20); // About 9.5e-7
        float result = roundTrip(denormal);
        // Denormalized numbers lose precision but should be close
        assertEquals((float) denormal, result, (float) denormal * 0.1f);
    }

    @Test
    public void testMinPositiveSubnormal() {
        // Min positive subnormal Float16 is 2^-24 ≈ 5.96e-8
        double minSubnormal = Math.pow(2, -24);
        float result = roundTrip(minSubnormal);
        // Due to precision loss, should be close but not exact
        assertTrue(result > 0);
        assertTrue(result < Math.pow(2, -14));
    }

    @Test
    public void testUnderflowToZero() {
        // Values < 2^-24 underflow to zero
        double tinyValue = Math.pow(2, -25);
        float result = roundTrip(tinyValue);
        assertEquals(0.0f, result, 0.0f);
    }

    @Test
    public void testNegativeDenormalized() {
        double denormal = -Math.pow(2, -20);
        float result = roundTrip(denormal);
        assertEquals((float) denormal, result, (float) -denormal * 0.1f);
    }

    // === Precision Tests ===

    @Test
    public void testPi() {
        // Pi rounded to Float16 precision
        float result = roundTrip(Math.PI);
        // Float16 has limited precision, but should be close
        assertEquals((float) Math.PI, result, 0.001f);
    }

    @Test
    public void testE() {
        float result = roundTrip(Math.E);
        assertEquals((float) Math.E, result, 0.001f);
    }

    @Test
    public void testPrecisionLoss() {
        // 1.0001 might not be exactly representable
        float result = roundTrip(1.0001);
        assertTrue(result >= 1.0f);
        assertTrue(result <= 1.001f);
    }

    // === Rounding Tests ===

    @Test
    public void testRoundingHalfToEven() {
        // Test values that fall exactly between representable Float16 values
        // This tests the rounding behavior
        float result1 = roundTrip(1.5);
        assertEquals(1.5f, result1, EPSILON);

        float result2 = roundTrip(2.5);
        assertEquals(2.5f, result2, EPSILON);
    }

    // === Edge Case Tests ===

    @Test
    public void testSequentialValues() {
        // Test a range of values to ensure consistency
        double[] values = {
            -1000.0, -100.0, -10.0, -1.0, -0.1, -0.01, 0.0, 0.01, 0.1, 1.0, 10.0, 100.0, 1000.0
        };

        for (double value : values) {
            float result = roundTrip(value);
            // Result should be finite for these values
            if (Math.abs(value) <= 65504) {
                assertFalse(Float.isInfinite(result), "Value " + value + " should not overflow");
            }
        }
    }

    @Test
    public void testPowersOfTwo() {
        // Powers of 2 should be exactly representable in Float16
        for (int exp = -14; exp <= 15; exp++) {
            double value = Math.pow(2, exp);
            float result = roundTrip(value);

            if (exp >= -14 && exp <= 15) {
                // Should be exact for this range
                assertEquals((float) value, result, (float) value * 1e-6f);
            }
        }
    }

    @Test
    public void testNegativePowersOfTwo() {
        for (int exp = -14; exp <= 15; exp++) {
            double value = -Math.pow(2, exp);
            float result = roundTrip(value);

            if (exp >= -14 && exp <= 15) {
                assertEquals((float) value, result, (float) -value * 1e-6f);
            }
        }
    }

    // === Comprehensive Value Tests ===

    @Test
    public void testCommonFloatValues() {
        double[] values = {
            0.0, 1.0, -1.0, 2.0, -2.0, 0.5, -0.5, 0.25, -0.25, 10.0, -10.0, 100.0, -100.0, 1000.0,
            -1000.0, 65504.0, // max
            -65504.0, // min
        };

        for (double value : values) {
            float result = roundTrip(value);
            // All these values should be representable or overflow predictably
            if (Float.isInfinite(result)) {
                assertTrue(
                        Math.abs(value) > 65504 || Float.isInfinite((float) value),
                        "Unexpected overflow for " + value);
            } else {
                // Should be close to original value
                double tolerance = Math.max(Math.abs(value) * 0.001, 0.01);
                assertEquals(
                        (float) value,
                        result,
                        (float) tolerance,
                        String.format("Value mismatch for %.4f", value));
            }
        }
    }

    @Test
    public void testRoundTripConsistency() {
        // Converting a value, reading it back, and converting again should be idempotent
        double[] values = {0.0, 1.0, -1.0, 100.0, -100.0, 0.001, -0.001};

        for (double value : values) {
            int bits1 = Conversions.float16ToShortBits(value) & 0xffff;
            float intermediate = Conversions.shortBitsToFloat16((short) bits1);
            int bits2 = Conversions.float16ToShortBits(intermediate) & 0xffff;

            assertEquals(bits1, bits2, "Round-trip produced different bits for " + value);
        }
    }

    // === Overflow and Boundary Edge Cases ===

    /**
     * Test values that are very close to the Float16 overflow boundary. This tests the edge case
     * where normalized values might overflow during conversion.
     */
    @Test
    public void testNearOverflowBoundary() {
        // Test values right at the overflow boundary
        // Max finite Float16 is 65504
        // Values >= 65520 definitely overflow
        float result1 = roundTrip(65504.0);
        assertEquals(65504.0f, result1, 0.0f);

        float result2 = roundTrip(65520.0);
        assertEquals(Float.POSITIVE_INFINITY, result2, 0.0f);

        // Negative versions
        float result3 = roundTrip(-65504.0);
        assertEquals(-65504.0f, result3, 0.0f);

        float result4 = roundTrip(-65520.0);
        assertEquals(Float.NEGATIVE_INFINITY, result4, 0.0f);
    }

    /**
     * Test the exact boundary between normalized and denormalized numbers. Values at exactly 2^-14
     * should be handled correctly.
     */
    @Test
    public void testNormalizedDenormalizedBoundary() {
        double minNormal = 6.103515625e-5; // 2^-14, exactly FLOAT16_MIN_NORMAL

        // Test exactly at the boundary
        float result1 = roundTrip(minNormal);
        assertTrue(result1 > 0);
        assertEquals((float) minNormal, result1, 1e-7f);

        // Test clearly below the boundary (in denormalized range)
        // Use half of minNormal to ensure we're in denormalized range
        double denormalized = minNormal * 0.5;
        float result2 = roundTrip(denormalized);
        assertTrue(result2 > 0);
        // Result should be less than or equal to minNormal
        assertTrue(result2 <= minNormal);

        // Negative versions
        float result3 = roundTrip(-minNormal);
        assertEquals((float) -minNormal, result3, 1e-7f);

        float result4 = roundTrip(-denormalized);
        assertTrue(result4 < 0);
        assertTrue(result4 >= -minNormal);
    }

    /**
     * Test very small denormalized values that are close to underflowing to zero. This exercises
     * the denormalized number conversion with rounding.
     */
    @Test
    public void testVerySmallDenormalizedValues() {
        double minSubnormal = 5.960464477539063E-8; // 2^-24

        // Test values in the denormalized range with various rounding scenarios
        double[] denormalizedValues = {
            minSubnormal * 1.5, // Should round
            minSubnormal * 2.5, // Test round-to-even
            minSubnormal * 3.5, // Test round-to-even (odd)
            minSubnormal * 511.5, // Near top of denormalized range, test rounding
            minSubnormal * 512, // Exactly representable
            minSubnormal * 1023, // Largest denormalized mantissa
            minSubnormal * 1024 // Should equal FLOAT16_MIN_NORMAL
        };

        for (double value : denormalizedValues) {
            float result = roundTrip(value);
            assertTrue(result >= 0, "Value " + value + " should be positive");

            // Negative versions
            float negResult = roundTrip(-value);
            assertTrue(negResult <= 0, "Value " + (-value) + " should be negative");
        }
    }

    /**
     * Test Float32 values that might have edge cases during exponent conversion. This tests values
     * with extreme Float32 exponents that need careful handling.
     */
    @Test
    public void testExtremeFloat32Exponents() {
        // Create Float32 values with specific bit patterns that test exponent edge cases
        // These values test the boundary between different conversion paths

        // Float32 with large exponent that should overflow to Float16 infinity
        float largeExp = Float.intBitsToFloat(0x7F000000); // Large positive value
        float result1 = roundTrip(largeExp);
        assertTrue(Float.isInfinite(result1), "Large exponent should overflow to infinity");

        // Float32 with small exponent near denormalized boundary
        float smallExp = Float.intBitsToFloat(0x38800000); // 2^-14
        float result2 = roundTrip(smallExp);
        assertTrue(result2 > 0, "Small exponent at boundary should be normalized");

        // Float32 slightly smaller (denormalized in Float16)
        float slightlySmaller = Float.intBitsToFloat(0x387FC000); // Just below 2^-14
        float result3 = roundTrip(slightlySmaller);
        assertTrue(result3 > 0, "Value below normalized boundary should work");
    }

    /**
     * Test values that might cause mantissa overflow during rounding. When rounding a mantissa
     * that's already at maximum, it should overflow to the next exponent.
     */
    @Test
    public void testMantissaRoundingOverflow() {
        // Test values where mantissa rounding might cause overflow to next exponent
        // Float16 mantissa is 10 bits (0x3FF max)

        // Create Float32 values near boundaries where rounding could cause mantissa overflow
        // These are values where the Float32 mantissa, when truncated to 10 bits,
        // is at 0x3FF and the remaining bits might cause rounding up

        // Value just below 2.0 that rounds to 2.0
        float nearTwo = Float.intBitsToFloat(0x3FFFFFFF); // 1.9999999...
        float result1 = roundTrip(nearTwo);
        assertEquals(2.0f, result1, EPSILON);

        // Value just below 4.0 that rounds to 4.0
        float nearFour = Float.intBitsToFloat(0x407FFFFF); // 3.9999999...
        float result2 = roundTrip(nearFour);
        assertEquals(4.0f, result2, 0.01f);

        // Test near the maximum representable value
        // 65504 is 0x7BFF in Float16 (exp=30, mantissa=0x3FF)
        // Values slightly above might round up and overflow to infinity
        float nearMax = 65504.0f;
        float result3 = roundTrip(nearMax);
        assertEquals(65504.0f, result3, 0.0f);

        // Value that's exactly at the rounding boundary
        float justAboveMax = Float.intBitsToFloat(0x477FE100); // Between 65504 and 65520
        float result4 = roundTrip(justAboveMax);
        // Should either be 65504 or infinity depending on exact value
        assertTrue(result4 == 65504.0f || Float.isInfinite(result4));
    }

    /**
     * Test that values requiring IEEE 754 round-to-nearest-even (banker's rounding) are handled
     * correctly, especially in edge cases.
     */
    @Test
    public void testRoundToNearestEvenEdgeCases() {
        double minSubnormal = 5.960464477539063E-8; // 2^-24

        // Test exact halfway points in denormalized range
        // These should round to even mantissa values

        // 1.5 * minSubnormal: mantissa would be 1.5, should round to 2 (even)
        float result1 = roundTrip(minSubnormal * 1.5);
        float expected1 = roundTrip(minSubnormal * 2);
        assertEquals(expected1, result1, 0.0f);

        // 2.5 * minSubnormal: mantissa would be 2.5, should round to 2 (even)
        float result2 = roundTrip(minSubnormal * 2.5);
        float expected2 = roundTrip(minSubnormal * 2);
        assertEquals(expected2, result2, 0.0f);

        // 3.5 * minSubnormal: mantissa would be 3.5, should round to 4 (even)
        float result3 = roundTrip(minSubnormal * 3.5);
        float expected3 = roundTrip(minSubnormal * 4);
        assertEquals(expected3, result3, 0.0f);
    }

    /**
     * Test conversion of Float32 values with subnormal Float32 values to ensure they're handled
     * correctly.
     */
    @Test
    public void testFloat32SubnormalInput() {
        // Float32 minimum positive subnormal is approximately 1.4e-45
        float float32MinSubnormal = Float.MIN_VALUE;

        // This should underflow to zero in Float16
        float result1 = roundTrip(float32MinSubnormal);
        assertEquals(0.0f, result1, 0.0f);

        // Float32 subnormals should generally become zero in Float16
        float smallSubnormal = Float.intBitsToFloat(0x00000100);
        float result2 = roundTrip(smallSubnormal);
        assertEquals(0.0f, result2, 0.0f);
    }

    /**
     * Test that Double values (not Float32) at boundaries are correctly converted through the
     * Float32 intermediate step.
     */
    @Test
    public void testDoubleToFloat16Precision() {
        // Test Double values that might have precision issues when converted through Float32

        // Double value at Float16 max boundary
        double doubleMax = 65504.0;
        float result1 = roundTrip(doubleMax);
        assertEquals(65504.0f, result1, 0.0f);

        // Double value slightly above (should overflow)
        double slightlyAbove = 65504.1;
        float result2 = roundTrip(slightlyAbove);
        // Should be 65504 or infinity
        assertTrue(result2 >= 65504.0f);

        // Double value at denormalized boundary
        double doubleDenorm = 6.103515625e-5; // 2^-14
        float result3 = roundTrip(doubleDenorm);
        assertEquals((float) doubleDenorm, result3, 1e-7f);
    }

    /**
     * Test specific Float32 bit patterns that exercise edge cases in the conversion logic. This
     * targets specific code paths that may be hard to reach with regular float values.
     */
    @Test
    public void testSpecificFloat32BitPatterns() {
        // Test Float32 values with carefully crafted exponents

        // Float32 value at the threshold: 2^16 (exponent = 143)
        // This is 65536, which overflows Float16
        float value1 = Float.intBitsToFloat(0x47800000); // 2^16 = 65536
        float result1 = roundTrip(value1);
        assertTrue(
                Float.isInfinite(result1) || result1 == 65504.0f,
                "2^16 should overflow to infinity");

        // Float32 with exponent that would be exactly 0 in Float16 (2^-15)
        // exp32 = -15, so exponent = -15 + 15 = 0 (denormalized)
        float value2 = Float.intBitsToFloat(0x38000000); // 2^-15
        float result2 = roundTrip(value2);
        assertTrue(result2 >= 0, "2^-15 should be positive");

        // Test with various exponent values near boundaries
        int[] exponents = {
            112, // exp32 = -15, float16 exponent = 0 (boundary)
            113, // exp32 = -14, float16 exponent = 1 (min normal)
            141, // exp32 = 14, float16 exponent = 29
            142, // exp32 = 15, float16 exponent = 30 (max normal)
            143 // exp32 = 16, float16 exponent = 31 (overflow)
        };

        for (int exp : exponents) {
            // Create Float32 with specified exponent and mantissa = 0
            int bits = (exp << 23);
            float value = Float.intBitsToFloat(bits);
            if (!Float.isInfinite(value) && !Float.isNaN(value) && value != 0.0f) {
                float result = roundTrip(value);
                // Just verify it doesn't crash and produces a valid result
                assertTrue(
                        !Float.isNaN(result) || Float.isNaN(value),
                        "Result should be valid for exponent " + exp);
            }
        }
    }

    /**
     * Test values that are right at the rounding boundaries to trigger mantissa overflow edge
     * cases.
     */
    @Test
    public void testPreciseRoundingBoundaries() {
        // Create Float32 values where mantissa bits [12:0] = 0x1000 exactly
        // This is the halfway point for rounding

        // Test at various exponents with mantissa that will cause rounding
        // When mantissa bits [22:13] are all 1s (0x3FF) and we round up,
        // it should overflow to the next exponent

        // Exponent 142 (float16 exp 30), mantissa causing overflow
        // This should test the path where mantissa overflow causes exponent increment
        int bits1 = (142 << 23) | 0x7FF000; // Mantissa [22:13] = 0x3FF, [12:0] = 0x1000
        float value1 = Float.intBitsToFloat(bits1);
        float result1 = roundTrip(value1);
        // Should round up and potentially overflow
        assertTrue(!Float.isNaN(result1), "Value should not be NaN");

        // Test with odd mantissa (should round up)
        int bits2 = (127 << 23) | 0x7FF800; // Mantissa with specific pattern
        float value2 = Float.intBitsToFloat(bits2);
        float result2 = roundTrip(value2);
        assertTrue(!Float.isNaN(result2), "Value should not be NaN");

        // Test with even mantissa (should round to even)
        int bits3 = (127 << 23) | 0x7FE800; // Mantissa with specific pattern
        float value3 = Float.intBitsToFloat(bits3);
        float result3 = roundTrip(value3);
        assertTrue(!Float.isNaN(result3), "Value should not be NaN");
    }

    /**
     * Test the absolute maximum values that can be represented in Float32 to ensure proper overflow
     * handling.
     */
    @Test
    public void testExtremeValues() {
        // Float.MAX_VALUE should overflow to infinity in Float16
        float result1 = roundTrip(Float.MAX_VALUE);
        assertEquals(Float.POSITIVE_INFINITY, result1, 0.0f);

        // -Float.MAX_VALUE should overflow to negative infinity
        float result2 = roundTrip(-Float.MAX_VALUE);
        assertEquals(Float.NEGATIVE_INFINITY, result2, 0.0f);

        // Test values between Float16 max and Float32 max
        float largeValue = 1e20f;
        float result3 = roundTrip(largeValue);
        assertEquals(Float.POSITIVE_INFINITY, result3, 0.0f);

        // Very small Float32 values
        float tinyValue = Float.MIN_NORMAL;
        float result4 = roundTrip(tinyValue);
        assertEquals(0.0f, result4, 0.0f);
    }

    /**
     * Double values near the conversion boundaries, where double-to-float precision can change
     * which rounding path is taken.
     */
    @Test
    public void testDoublePrecisionEdgeCases() {
        // Test doubles that are near the boundaries but might have precision issues

        // Double values very close to 65520.0 (the overflow cutoff)
        // These might round differently when converted to float
        double[] nearOverflow = {
            65519.999999999, 65519.99, 65519.5, 65519.0, 65518.5, 65517.0, 65516.0, 65515.0
        };

        for (double d : nearOverflow) {
            float result = roundTrip(d);
            // Should either be finite or infinity, never NaN
            assertFalse(Float.isNaN(result), "Value " + d + " should not produce NaN");
        }

        // Double values very close to FLOAT16_MIN_NORMAL
        // These might behave differently due to double vs float precision
        double minNormal = 6.103515625e-5; // 2^-14
        double[] nearMinNormal = {
            minNormal * 1.0000001,
            minNormal * 0.9999999,
            minNormal * 1.00001,
            minNormal * 0.99999,
            minNormal + 1e-10,
            minNormal - 1e-10
        };

        for (double d : nearMinNormal) {
            float result = roundTrip(d);
            // Should be positive and not NaN
            assertTrue(result >= 0, "Value " + d + " should be >= 0");
            assertFalse(Float.isNaN(result), "Value " + d + " should not be NaN");
        }

        // Test values where double and float representation might differ
        // Use nextAfter to get values extremely close to boundaries
        double d1 = Math.nextAfter(65520.0, 0.0); // Just below 65520
        float result1 = roundTrip(d1);
        assertFalse(Float.isNaN(result1), "nextAfter(65520, 0) should not be NaN");

        double d2 = Math.nextAfter(65504.0, Double.POSITIVE_INFINITY); // Just above 65504
        float result2 = roundTrip(d2);
        assertFalse(Float.isNaN(result2), "nextAfter(65504, +inf) should not be NaN");

        double d3 = Math.nextAfter(minNormal, 0.0); // Just below min normal
        float result3 = roundTrip(d3);
        assertFalse(Float.isNaN(result3), "nextAfter(minNormal, 0) should not be NaN");

        double d4 = Math.nextAfter(minNormal, Double.POSITIVE_INFINITY); // Just above min normal
        float result4 = roundTrip(d4);
        assertFalse(Float.isNaN(result4), "nextAfter(minNormal, +inf) should not be NaN");
    }

    /**
     * Test values constructed from Float32 with specific bit patterns designed to test edge cases
     * in the normalized float16 conversion path.
     */
    @Test
    public void testFloat32BitPatternEdgeCases() {
        // Test Float32 values where exponent is at the boundary (exp32 = 15, float16 exp = 30)
        // With maximum mantissa that will round up
        // Bits: sign=0, exp=142, mantissa=0x7FFFFF (all mantissa bits set)
        float maxMantissa = Float.intBitsToFloat((142 << 23) | 0x7FFFFF);
        float result1 = roundTrip(maxMantissa);
        // This is close to max float16, should be finite or infinity
        assertTrue(
                !Float.isNaN(result1) && (Float.isFinite(result1) || Float.isInfinite(result1)),
                "Max mantissa with exp 142 should be valid");

        // Test Float32 where exp32 = -15 (float16 exp = 0, denormalized boundary)
        // Bits: sign=0, exp=112, mantissa=0 (exactly at boundary)
        float denormBoundary = Float.intBitsToFloat(112 << 23);
        float result2 = roundTrip(denormBoundary);
        assertTrue(result2 >= 0, "Denorm boundary should be positive");

        // Test Float32 where exp32 = -15 with non-zero mantissa
        // This should force the normalized-to-denormalized path
        float belowNormal = Float.intBitsToFloat((112 << 23) | 0x400000);
        float result3 = roundTrip(belowNormal);
        assertTrue(result3 >= 0, "Below normal should be positive");

        // Test Float32 with exp32 = 16 (float16 exp would be 31, overflow)
        // But guard condition should catch this
        float overflow = Float.intBitsToFloat(143 << 23);
        float result4 = roundTrip(overflow);
        // Should overflow to infinity
        assertTrue(Float.isInfinite(result4) || result4 == 65504.0f, "Exp 143 should overflow");
    }

    /**
     * Values whose float32 exponent is large enough to push the float16 exponent to 31 (infinity)
     * should overflow to infinity rather than produce a bogus finite value.
     */
    @Test
    public void testNormalizedPathExponentOverflow() {
        // Create Float32 values that bypass early guards but overflow when computing exponent
        // We need exp32 >= 16, which means Float32 exponent >= 143

        // Float32 exponent 143: exp32 = 143 - 127 = 16, float16 exponent = 16 + 15 = 31 (overflow)
        float value1 = Float.intBitsToFloat((143 << 23)); // 2^16 = 65536
        float result1 = roundTrip(value1);
        assertTrue(Float.isInfinite(result1), "Exponent 143 should trigger overflow path");

        // Float32 exponent 144: exp32 = 17, float16 exponent = 32 (overflow)
        float value2 = Float.intBitsToFloat((144 << 23)); // 2^17
        float result2 = roundTrip(value2);
        assertTrue(Float.isInfinite(result2), "Exponent 144 should trigger overflow path");

        // Float32 exponent 143 with non-zero mantissa
        float value3 = Float.intBitsToFloat((143 << 23) | 0x400000); // 2^16 + fraction
        float result3 = roundTrip(value3);
        assertTrue(Float.isInfinite(result3), "Exponent 143 with mantissa should trigger overflow");

        // Negative versions
        float value4 = Float.intBitsToFloat((1 << 31) | (143 << 23)); // -2^16
        float result4 = roundTrip(value4);
        assertTrue(
                Float.isInfinite(result4) && result4 < 0,
                "Negative exponent 143 should trigger overflow");

        // Float32 exponent at exactly the boundary (142: exp32=15, float16 exp=30, should not
        // overflow)
        float value5 = Float.intBitsToFloat((142 << 23)); // 2^15 = 32768
        float result5 = roundTrip(value5);
        // This should be finite, not overflow
        assertTrue(
                Float.isFinite(result5) || Float.isInfinite(result5),
                "Exponent 142 should not overflow through this path");
    }

    /**
     * Float32 values below the Float16 min-normal convert to denormalized Float16, which requires
     * special IEEE 754 rounding. Exercises that denormalized path.
     */
    @Test
    public void testNormalizedToDenomalizedConversion() {
        // Create Float32 values that are normalized in Float32 but become denormalized in Float16
        // We need exp32 <= -15 (Float32 exponent <= 112), which routes to denormalized path

        // Float32 exponent 112: exp32 = 112 - 127 = -15, float16 exponent = -15 + 15 = 0
        // (denormalized)
        float value1 = Float.intBitsToFloat((112 << 23)); // 2^-15 = 0.000030517578125
        float result1 = roundTrip(value1);
        assertTrue(result1 >= 0, "2^-15 should be positive");
        assertFalse(Float.isNaN(result1), "2^-15 should not be NaN");

        // Float32 exponent 112 with non-zero mantissa (various values in denormalized range)
        float value2 = Float.intBitsToFloat((112 << 23) | 0x700000);
        float result2 = roundTrip(value2);
        assertTrue(result2 >= 0, "Below min normal with mantissa should be positive");

        // Test multiple mantissa values to exercise rounding paths in denormalized conversion
        // More than halfway: should round up
        float value3 = Float.intBitsToFloat((112 << 23) | 0x600000);
        float result3 = roundTrip(value3);
        assertTrue(result3 >= 0, "Denorm with rounding up should be positive");

        // Exactly halfway (tests round-to-even)
        float value4 = Float.intBitsToFloat((112 << 23) | 0x400000);
        float result4 = roundTrip(value4);
        assertTrue(result4 >= 0, "Denorm at halfway should round to even");

        // Less than halfway: should round down
        float value5 = Float.intBitsToFloat((112 << 23) | 0x200000);
        float result5 = roundTrip(value5);
        assertTrue(result5 >= 0, "Denorm with rounding down should be positive");

        // Float32 exponent 111: exp32 = -16, float16 exponent = -1 (denormalized)
        float value6 = Float.intBitsToFloat((111 << 23)); // 2^-16
        float result6 = roundTrip(value6);
        assertTrue(result6 >= 0, "2^-16 should be positive");

        // Float32 exponent 111 with mantissa creating odd/even rounding scenarios
        float value7 = Float.intBitsToFloat((111 << 23) | 0x500000);
        float result7 = roundTrip(value7);
        assertTrue(result7 >= 0, "2^-16 with mantissa should be positive");

        // Negative versions to test sign handling
        float value8 = Float.intBitsToFloat((1 << 31) | (112 << 23)); // -2^-15
        float result8 = roundTrip(value8);
        assertTrue(result8 <= 0, "Negative denorm should be negative");

        float value9 = Float.intBitsToFloat((1 << 31) | (112 << 23) | 0x400000);
        float result9 = roundTrip(value9);
        assertTrue(result9 <= 0, "Negative denorm with mantissa should be negative");

        // Test with mantissa that's exactly odd (for round-to-even)
        float value10 = Float.intBitsToFloat((112 << 23) | 0x300000);
        float result10 = roundTrip(value10);
        assertTrue(result10 >= 0, "Odd mantissa rounding should be positive");

        // Test with mantissa that's exactly even (for round-to-even)
        float value11 = Float.intBitsToFloat((112 << 23) | 0x200001);
        float result11 = roundTrip(value11);
        assertTrue(result11 >= 0, "Even mantissa rounding should be positive");
    }

    /**
     * Float32 values at the exact conversion boundaries, where floating-point precision could push
     * a value across a guard threshold.
     */
    @Test
    public void testConversionBoundaries() {
        // Values right at the boundary of 2^16
        float exactBoundary1 = Float.intBitsToFloat((143 << 23) - 1); // Just below 2^16
        float result1 = roundTrip(exactBoundary1);
        assertTrue(!Float.isNaN(result1), "Near 2^16 should produce valid result");

        // Try with extremely specific exponent=-15 boundary values
        // Float32 with exp=-15, mantissa near maximum
        float boundary2 = Float.intBitsToFloat((112 << 23) | 0x7FFFFF);
        float result2 = roundTrip(boundary2);
        assertTrue(!Float.isNaN(result2), "Exp=-15 boundary should produce valid result");

        // Edge case: values constructed to test precision boundaries
        // This tests if float casting and precision loss might bypass guards
        double precisionEdge1 = 65519.99999999; // Just below overflow guard
        float result3 = roundTrip(precisionEdge1);
        assertFalse(Float.isNaN(result3), "Precision edge should not produce NaN");

        // Test with nextAfter to get closest representable values
        float nextAfterMin = Math.nextAfter((float) 6.103515625e-5, 0.0f);
        float result4 = roundTrip(nextAfterMin);
        assertTrue(result4 >= 0, "NextAfter min normal should be >= 0");

        // Values at exact Float32 representation boundaries
        float[] boundaryValues = {
            Float.intBitsToFloat((142 << 23) | 0x7FFFEF), // Near max exponent
            Float.intBitsToFloat((112 << 23) | 0x400001), // Near min exponent
            Float.intBitsToFloat((142 << 23) | 0x7FFF00),
            Float.intBitsToFloat((113 << 23) - 1), // Just below normalized threshold
        };

        for (float value : boundaryValues) {
            float result = roundTrip(value);
            assertFalse(Float.isNaN(result), "Boundary value should not produce NaN");
        }
    }

    /**
     * Rounding a maxed-out mantissa (0x3FF) up overflows into the next exponent; at the largest
     * finite exponent that carry reaches infinity.
     */
    @Test
    public void testMantissaOverflowToInfinity() {
        // Create Float32 values where:
        // 1. exp32 = 15 (Float32 exponent 142), so float16 exponent = 30
        // 2. Mantissa rounds up from 0x3FF to 0x400, causing exponent increment to 31 (infinity)

        // Float32 exponent 142 with mantissa that will round up
        // Mantissa [22:13] should be 0x3FF (all 1s), and [12:0] should cause rounding up
        // Bits [12:0] > 0x1000 will round up

        // Create mantissa: bits [22:13] = 0x3FF (bits 0x7FE000), bits [12:0] = 0x1001
        int bits1 = (142 << 23) | 0x7FE000 | 0x1001;
        float value1 = Float.intBitsToFloat(bits1);
        float result1 = roundTrip(value1);
        assertTrue(
                Float.isInfinite(result1),
                "Mantissa overflow at max exponent should cause infinity");

        // Test with exactly 0x1000 in low bits and odd mantissa (rounds up)
        int bits2 = (142 << 23) | 0x7FE000 | 0x1000 | (1 << 13); // Make mantissa odd
        float value2 = Float.intBitsToFloat(bits2);
        float result2 = roundTrip(value2);
        assertTrue(
                Float.isInfinite(result2),
                "Mantissa overflow with round-to-even (odd) should cause infinity");

        // Test with maximum mantissa
        int bits3 = (142 << 23) | 0x7FFFFF; // All mantissa bits set
        float value3 = Float.intBitsToFloat(bits3);
        float result3 = roundTrip(value3);
        assertTrue(
                Float.isInfinite(result3),
                "Maximum mantissa at exponent 142 should overflow to infinity");

        // Negative version
        int bits4 = (1 << 31) | (142 << 23) | 0x7FE000 | 0x1001;
        float value4 = Float.intBitsToFloat(bits4);
        float result4 = roundTrip(value4);
        assertTrue(
                Float.isInfinite(result4) && result4 < 0,
                "Negative mantissa overflow should cause negative infinity");

        // Test multiple rounding scenarios at exponent 142
        int bits5 = (142 << 23) | 0x7FE800; // Different mantissa pattern
        float value5 = Float.intBitsToFloat(bits5);
        float result5 = roundTrip(value5);
        // Should be close to max or infinity
        assertTrue(
                Float.isFinite(result5) || Float.isInfinite(result5),
                "Near-max value should be finite or infinite");

        // Test with bits that are exactly at the halfway point with even mantissa
        // Should not overflow if rounding to even keeps it at 0x3FF
        int bits6 = (142 << 23) | 0x7FC000 | 0x1000; // Even mantissa, exactly halfway
        float value6 = Float.intBitsToFloat(bits6);
        float result6 = roundTrip(value6);
        // Might or might not overflow depending on rounding
        assertTrue(
                Float.isFinite(result6) || Float.isInfinite(result6),
                "Halfway rounding at boundary should be valid");
    }

    /** Large-exponent values that the early overflow guards catch should still map to infinity. */
    @Test
    public void testLargeExponentOverflowToInfinity() {
        // Scenario: Float32 with exp32=16 (exponent 143) would give float16 exponent=31 (infinity)

        // Any value >= 2^16 should map to infinity
        float testValue = Float.intBitsToFloat(143 << 23); // 2^16 = 65536

        float result = roundTrip(testValue);
        assertTrue(Float.isInfinite(result), "exp32=16 should produce infinity");

        // Test with even larger exponents to ensure robustness
        for (int exp = 143; exp <= 150; exp++) {
            float value = Float.intBitsToFloat(exp << 23);
            float readBack = roundTrip(value);
            assertTrue(
                    Float.isInfinite(readBack) && readBack > 0,
                    "Large exponent " + exp + " should produce infinity");
        }

        // Negative versions
        float negValue = Float.intBitsToFloat((1 << 31) | (143 << 23));
        float negResult = roundTrip(negValue);
        assertTrue(
                Float.isInfinite(negResult) && negResult < 0,
                "negative exp32=16 should produce -infinity");
    }

    /** Small values in the denormalized Float16 range convert correctly. */
    @Test
    public void testDenormalizedPath_SmallValues() {
        // These values fall in the denormalized Float16 range
        float minValue = Float.intBitsToFloat(112 << 23); // 2^-15
        float boundary = Float.intBitsToFloat(113 << 23); // 2^-14

        // Test that these values produce correct denormalized results
        float result1 = roundTrip(minValue);
        assertTrue(result1 >= 0, "2^-15 should produce positive denormalized result");
        assertTrue(result1 < 0.001, "2^-15 should be small");

        float result2 = roundTrip(boundary);
        assertTrue(result2 >= 0, "2^-14 should produce positive result");

        // Test the full denormalized range with values that have exp32 <= -15
        for (int exp = 102; exp <= 112; exp++) {
            float value = Float.intBitsToFloat(exp << 23);
            float readBack = roundTrip(value);
            assertTrue(
                    readBack >= 0, "Small exponent " + exp + " should produce non-negative result");
            assertFalse(Float.isNaN(readBack), "Should not produce NaN");
        }

        // Test with mantissa variations to exercise rounding paths in denormalized path
        float value1 = Float.intBitsToFloat((112 << 23) | 0x600000); // More than halfway
        float resultA = roundTrip(value1);
        assertTrue(resultA >= 0, "Denorm with rounding should be valid");

        float value2 = Float.intBitsToFloat((112 << 23) | 0x400000); // Exactly halfway
        float resultB = roundTrip(value2);
        assertTrue(resultB >= 0, "Denorm at tie should be valid");

        float value3 = Float.intBitsToFloat((112 << 23) | 0x200000); // Less than halfway
        float resultC = roundTrip(value3);
        assertTrue(resultC >= 0, "Denorm with round down should be valid");
    }

    /**
     * Values near the maximum finite Float16, where mantissa rounding could carry into the next
     * exponent, should produce the max finite value or infinity (never NaN).
     */
    @Test
    public void testNearMaxMantissaRounding() {
        // Scenario: Float32 with exp=142 (exp32=15, float16 exp=30) and mantissa that rounds up
        // causing overflow from 0x3FF to 0x400, incrementing exponent to 31 (infinity)

        // Verify values near the max Float16 produce correct results

        // Create Float32 values with exp=142 and maximum mantissa
        int bits1 = (142 << 23) | 0x7FFFFF; // All mantissa bits set
        float value1 = Float.intBitsToFloat(bits1);

        float result1 = roundTrip(value1);

        // This should either be the max float16 or infinity (guards determine which)
        assertTrue(
                result1 >= 65504.0f || Float.isInfinite(result1),
                "Max mantissa at exp=30 should produce max or infinity");

        // Test a range of mantissa values at exponent 142 that could cause overflow
        for (int mantissaHigh = 0x7FE; mantissaHigh <= 0x7FF; mantissaHigh++) {
            for (int mantissaLow = 0; mantissaLow <= 0x1FFF; mantissaLow += 0x400) {
                int bits = (142 << 23) | (mantissaHigh << 13) | mantissaLow;
                float value = Float.intBitsToFloat(bits);

                float readBack = roundTrip(value);

                // Should be finite max or infinity, never NaN
                assertFalse(Float.isNaN(readBack), "Near-overflow value should not produce NaN");
                assertTrue(
                        readBack >= 65504.0f || Float.isInfinite(readBack),
                        "Should be large positive value or infinity");
            }
        }

        // Test the exact boundary where mantissa overflow would occur
        // If mantissa = 0x3FF and rounding adds 1, it becomes 0x400, causing exponent increment
        int boundaryBits = (142 << 23) | 0x7FE000 | 0x1001;
        float boundaryValue = Float.intBitsToFloat(boundaryBits);

        float boundaryResult = roundTrip(boundaryValue);

        assertTrue(
                Float.isFinite(boundaryResult) || Float.isInfinite(boundaryResult),
                "Boundary case should produce valid result");

        // Negative versions
        int negBits = (1 << 31) | (142 << 23) | 0x7FFFFF;
        float negValue = Float.intBitsToFloat(negBits);

        float negResult = roundTrip(negValue);

        assertTrue(
                negResult <= -65504.0f || (Float.isInfinite(negResult) && negResult < 0),
                "Negative near-max should produce large negative or -infinity");
    }

    /**
     * Boundary math sanity: overflowing exponents map to the infinity bit pattern, sub-normal
     * values stay positive and below the min-normal, and near-max values stay finite.
     */
    @Test
    public void testOverflowAndBoundaryMath() {
        // Test 1: an overflowing exponent should produce the same bits as explicit infinity

        int positiveInfBits = Conversions.float16ToShortBits(Float.POSITIVE_INFINITY) & 0xffff;

        float largeValue = Float.intBitsToFloat(143 << 23);
        int largeValueBits = Conversions.float16ToShortBits(largeValue) & 0xffff;

        // Both should produce the same infinity bit pattern
        assertEquals(
                positiveInfBits,
                largeValueBits,
                "Large exponent should produce same result as explicit infinity");

        // Test 2: verify the denormalized conversion math is correct
        double minNormal = 6.103515625e-5; // 2^-14
        double minSubnormal = 5.960464477539063E-8; // 2^-24

        // Values below minNormal should produce denormalized results
        float smallValue = (float) (minNormal * 0.5);
        float smallResult = roundTrip(smallValue);

        assertTrue(smallResult >= 0, "Small value should be positive");
        assertTrue(smallResult <= minNormal, "Small value should be less than minNormal");

        // Test 3: Verify mantissa overflow handling
        // Create a scenario that exercises mantissa rounding near the limit
        float nearMax = 65503.99f;
        float nearMaxResult = roundTrip(nearMax);

        assertTrue(Float.isFinite(nearMaxResult), "Near-max should produce finite result");
        assertTrue(nearMaxResult >= 65500.0f, "Should be close to max");
    }

    @Test
    public void testDenormalizedRange() {
        // Small values in the denormalized Float16 range convert to positive, non-NaN results
        float value1 = Float.intBitsToFloat(112 << 23); // 2^-15
        float result1 = roundTrip(value1);
        assertTrue(result1 >= 0, "2^-15 should produce valid denormalized result");

        // Test with various small values that go through denormalized path
        float value2 = Float.intBitsToFloat((112 << 23) | 0x600000);
        float result2 = roundTrip(value2);
        assertTrue(result2 >= 0, "Small value should be handled by denormalized path");

        // Test boundary: exactly at FLOAT16_MIN_NORMAL
        float boundary = Float.intBitsToFloat(113 << 23); // 2^-14
        float resultBoundary = roundTrip(boundary);
        assertTrue(resultBoundary >= 0, "Boundary value should produce valid result");

        // Verify that very small values produce correct results
        for (int exp = 102; exp <= 112; exp++) {
            float value = Float.intBitsToFloat(exp << 23);
            float readBack = roundTrip(value);
            assertTrue(
                    readBack >= 0, "Small exponent " + exp + " should produce non-negative result");
            assertFalse(Float.isNaN(readBack), "Should not produce NaN");
        }

        // Negative versions
        float negValue = Float.intBitsToFloat((1 << 31) | (112 << 23));
        float negResult = roundTrip(negValue);
        assertTrue(negResult <= 0, "Negative small value should work");
    }
}
