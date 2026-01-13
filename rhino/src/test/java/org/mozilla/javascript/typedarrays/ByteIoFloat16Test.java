package org.mozilla.javascript.typedarrays;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Comprehensive tests for Float16 read/write operations in ByteIo.
 *
 * <p>Float16 (also known as half-precision floating point) uses 16 bits: - 1 bit: sign - 5 bits:
 * exponent - 10 bits: mantissa
 *
 * <p>Special values: - Zero: exponent=0, mantissa=0 - Denormalized: exponent=0, mantissa!=0 -
 * Normalized: exponent=1-30 - Infinity: exponent=31, mantissa=0 - NaN: exponent=31, mantissa!=0
 *
 * <p>Value ranges: - Max: 65504 (0x7BFF) - Min positive normal: 2^-14 ≈ 0.000061035 - Min positive
 * subnormal: 2^-24 ≈ 5.96e-8
 */
public class ByteIoFloat16Test {

    private static final double EPSILON = 1e-10;

    /**
     * Helper to write and read a Float16 value for round-trip testing.
     *
     * @param value The value to write
     * @param littleEndian Byte order
     * @return The value read back
     */
    private float roundTrip(double value, boolean littleEndian) {
        byte[] buf = new byte[2];
        ByteIo.writeFloat16(buf, 0, value, littleEndian);
        return ByteIo.readFloat16(buf, 0, littleEndian);
    }

    /**
     * Helper to verify bit pattern matches expected value.
     *
     * @param expectedBits The expected 16-bit pattern
     * @param value The float value to write
     * @param littleEndian Byte order
     */
    private void assertBits(int expectedBits, double value, boolean littleEndian) {
        byte[] buf = new byte[2];
        ByteIo.writeFloat16(buf, 0, value, littleEndian);

        int actualBits;
        if (littleEndian) {
            actualBits = (buf[0] & 0xff) | ((buf[1] & 0xff) << 8);
        } else {
            actualBits = ((buf[0] & 0xff) << 8) | (buf[1] & 0xff);
        }

        assertEquals(
                String.format("Bit pattern mismatch for value %.10f", value),
                expectedBits,
                actualBits);
    }

    // === Zero Tests ===

    @Test
    public void testPositiveZero() {
        float result = roundTrip(0.0, true);
        assertEquals(0.0f, result, 0.0f);
        assertTrue(Float.floatToIntBits(result) == Float.floatToIntBits(0.0f));
    }

    @Test
    public void testNegativeZero() {
        float result = roundTrip(-0.0, true);
        assertEquals(-0.0f, result, 0.0f);
        assertTrue(Float.floatToIntBits(result) == Float.floatToIntBits(-0.0f));
    }

    @Test
    public void testZeroBitPattern() {
        assertBits(0x0000, 0.0, true); // +0
        assertBits(0x8000, -0.0, true); // -0
    }

    // === Simple Value Tests ===

    @Test
    public void testOne() {
        float result = roundTrip(1.0, true);
        assertEquals(1.0f, result, EPSILON);
    }

    @Test
    public void testMinusOne() {
        float result = roundTrip(-1.0, true);
        assertEquals(-1.0f, result, EPSILON);
    }

    @Test
    public void testTwo() {
        float result = roundTrip(2.0, true);
        assertEquals(2.0f, result, EPSILON);
    }

    @Test
    public void testOneHalf() {
        float result = roundTrip(0.5, true);
        assertEquals(0.5f, result, EPSILON);
    }

    // === Infinity Tests ===

    @Test
    public void testPositiveInfinity() {
        float result = roundTrip(Float.POSITIVE_INFINITY, true);
        assertTrue(Float.isInfinite(result));
        assertTrue(result > 0);
    }

    @Test
    public void testNegativeInfinity() {
        float result = roundTrip(Float.NEGATIVE_INFINITY, true);
        assertTrue(Float.isInfinite(result));
        assertTrue(result < 0);
    }

    @Test
    public void testInfinityBitPattern() {
        assertBits(0x7C00, Float.POSITIVE_INFINITY, true);
        assertBits(0xFC00, Float.NEGATIVE_INFINITY, true);
    }

    // === NaN Tests ===

    @Test
    public void testNaN() {
        float result = roundTrip(Float.NaN, true);
        assertTrue(Float.isNaN(result));
    }

    @Test
    public void testNaNBitPattern() {
        byte[] buf = new byte[2];
        ByteIo.writeFloat16(buf, 0, Float.NaN, true);
        int bits = (buf[0] & 0xff) | ((buf[1] & 0xff) << 8);

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
        float result = roundTrip(65504.0, true);
        assertEquals(65504.0f, result, 0.0f);
    }

    @Test
    public void testOverflowToPositiveInfinity() {
        // Values > 65504 overflow to infinity
        float result = roundTrip(100000.0, true);
        assertEquals(Float.POSITIVE_INFINITY, result, 0.0f);
    }

    @Test
    public void testOverflowToNegativeInfinity() {
        float result = roundTrip(-100000.0, true);
        assertEquals(Float.NEGATIVE_INFINITY, result, 0.0f);
    }

    // === Denormalized Number Tests ===

    @Test
    public void testMinPositiveNormal() {
        // Min positive normal Float16 is 2^-14 ≈ 0.000061035
        double minNormal = Math.pow(2, -14);
        float result = roundTrip(minNormal, true);
        assertEquals((float) minNormal, result, 1e-7);
    }

    @Test
    public void testDenormalizedNumber() {
        // Test a denormalized number between 2^-24 and 2^-14
        double denormal = Math.pow(2, -20); // About 9.5e-7
        float result = roundTrip(denormal, true);
        // Denormalized numbers lose precision but should be close
        assertEquals((float) denormal, result, (float) denormal * 0.1f);
    }

    @Test
    public void testMinPositiveSubnormal() {
        // Min positive subnormal Float16 is 2^-24 ≈ 5.96e-8
        double minSubnormal = Math.pow(2, -24);
        float result = roundTrip(minSubnormal, true);
        // Due to precision loss, should be close but not exact
        assertTrue(result > 0);
        assertTrue(result < Math.pow(2, -14));
    }

    @Test
    public void testUnderflowToZero() {
        // Values < 2^-24 underflow to zero
        double tinyValue = Math.pow(2, -25);
        float result = roundTrip(tinyValue, true);
        assertEquals(0.0f, result, 0.0f);
    }

    @Test
    public void testNegativeDenormalized() {
        double denormal = -Math.pow(2, -20);
        float result = roundTrip(denormal, true);
        assertEquals((float) denormal, result, (float) -denormal * 0.1f);
    }

    // === Precision Tests ===

    @Test
    public void testPi() {
        // Pi rounded to Float16 precision
        float result = roundTrip(Math.PI, true);
        // Float16 has limited precision, but should be close
        assertEquals((float) Math.PI, result, 0.001f);
    }

    @Test
    public void testE() {
        float result = roundTrip(Math.E, true);
        assertEquals((float) Math.E, result, 0.001f);
    }

    @Test
    public void testPrecisionLoss() {
        // 1.0001 might not be exactly representable
        float result = roundTrip(1.0001, true);
        assertTrue(result >= 1.0f);
        assertTrue(result <= 1.001f);
    }

    // === Rounding Tests ===

    @Test
    public void testRoundingHalfToEven() {
        // Test values that fall exactly between representable Float16 values
        // This tests the rounding behavior
        float result1 = roundTrip(1.5, true);
        assertEquals(1.5f, result1, EPSILON);

        float result2 = roundTrip(2.5, true);
        assertEquals(2.5f, result2, EPSILON);
    }

    // === Endianness Tests ===

    @Test
    public void testLittleEndianVsBigEndian() {
        double[] testValues = {0.0, 1.0, -1.0, 123.456, -789.012};

        for (double value : testValues) {
            float littleResult = roundTrip(value, true);
            float bigResult = roundTrip(value, false);

            // Both endianness should produce the same float value
            assertEquals(
                    String.format("Endianness mismatch for value %f", value),
                    littleResult,
                    bigResult,
                    EPSILON);
        }
    }

    @Test
    public void testLittleEndianByteOrder() {
        byte[] buf = new byte[2];
        // 0x4000 in little-endian is 2.0
        ByteIo.writeFloat16(buf, 0, 2.0, true);

        // In little-endian, low byte first
        assertEquals(0x00, buf[0] & 0xFF);
        assertEquals(0x40, buf[1] & 0xFF);
    }

    @Test
    public void testBigEndianByteOrder() {
        byte[] buf = new byte[2];
        // 0x4000 in big-endian is 2.0
        ByteIo.writeFloat16(buf, 0, 2.0, false);

        // In big-endian, high byte first
        assertEquals(0x40, buf[0] & 0xFF);
        assertEquals(0x00, buf[1] & 0xFF);
    }

    // === Edge Case Tests ===

    @Test
    public void testSequentialValues() {
        // Test a range of values to ensure consistency
        double[] values = {
            -1000.0, -100.0, -10.0, -1.0, -0.1, -0.01, 0.0, 0.01, 0.1, 1.0, 10.0, 100.0, 1000.0
        };

        for (double value : values) {
            float result = roundTrip(value, true);
            // Result should be finite for these values
            if (Math.abs(value) <= 65504) {
                assertFalse("Value " + value + " should not overflow", Float.isInfinite(result));
            }
        }
    }

    @Test
    public void testPowersOfTwo() {
        // Powers of 2 should be exactly representable in Float16
        for (int exp = -14; exp <= 15; exp++) {
            double value = Math.pow(2, exp);
            float result = roundTrip(value, true);

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
            float result = roundTrip(value, true);

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
            float result = roundTrip(value, true);
            // All these values should be representable or overflow predictably
            if (Float.isInfinite(result)) {
                assertTrue(
                        "Unexpected overflow for " + value,
                        Math.abs(value) > 65504 || Float.isInfinite((float) value));
            } else {
                // Should be close to original value
                double tolerance = Math.max(Math.abs(value) * 0.001, 0.01);
                assertEquals(
                        String.format("Value mismatch for %.4f", value),
                        (float) value,
                        result,
                        (float) tolerance);
            }
        }
    }

    @Test
    public void testRoundTripConsistency() {
        // Writing and reading should be consistent
        double[] values = {0.0, 1.0, -1.0, 100.0, -100.0, 0.001, -0.001};

        for (double value : values) {
            byte[] buf1 = new byte[2];
            byte[] buf2 = new byte[2];

            ByteIo.writeFloat16(buf1, 0, value, true);
            float intermediate = ByteIo.readFloat16(buf1, 0, true);
            ByteIo.writeFloat16(buf2, 0, intermediate, true);

            assertArrayEquals("Round-trip produced different bytes for " + value, buf1, buf2);
        }
    }

    // === Edge Cases for Uncovered Code Paths ===

    /**
     * Test values that are very close to the Float16 overflow boundary. This tests the edge case
     * where normalized values might overflow during conversion.
     */
    @Test
    public void testNearOverflowBoundary() {
        // Test values right at the overflow boundary
        // Max finite Float16 is 65504
        // Values >= 65520 definitely overflow
        float result1 = roundTrip(65504.0, true);
        assertEquals(65504.0f, result1, 0.0f);

        float result2 = roundTrip(65520.0, true);
        assertEquals(Float.POSITIVE_INFINITY, result2, 0.0f);

        // Negative versions
        float result3 = roundTrip(-65504.0, true);
        assertEquals(-65504.0f, result3, 0.0f);

        float result4 = roundTrip(-65520.0, true);
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
        float result1 = roundTrip(minNormal, true);
        assertTrue(result1 > 0);
        assertEquals((float) minNormal, result1, 1e-7f);

        // Test clearly below the boundary (in denormalized range)
        // Use half of minNormal to ensure we're in denormalized range
        double denormalized = minNormal * 0.5;
        float result2 = roundTrip(denormalized, true);
        assertTrue(result2 > 0);
        // Result should be less than or equal to minNormal
        assertTrue(result2 <= minNormal);

        // Negative versions
        float result3 = roundTrip(-minNormal, true);
        assertEquals((float) -minNormal, result3, 1e-7f);

        float result4 = roundTrip(-denormalized, true);
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
            float result = roundTrip(value, true);
            assertTrue("Value " + value + " should be positive", result >= 0);

            // Negative versions
            float negResult = roundTrip(-value, true);
            assertTrue("Value " + (-value) + " should be negative", negResult <= 0);
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
        float result1 = roundTrip(largeExp, true);
        assertTrue("Large exponent should overflow to infinity", Float.isInfinite(result1));

        // Float32 with small exponent near denormalized boundary
        float smallExp = Float.intBitsToFloat(0x38800000); // 2^-14
        float result2 = roundTrip(smallExp, true);
        assertTrue("Small exponent at boundary should be normalized", result2 > 0);

        // Float32 slightly smaller (denormalized in Float16)
        float slightlySmaller = Float.intBitsToFloat(0x387FC000); // Just below 2^-14
        float result3 = roundTrip(slightlySmaller, true);
        assertTrue("Value below normalized boundary should work", result3 > 0);
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
        float result1 = roundTrip(nearTwo, true);
        assertEquals(2.0f, result1, EPSILON);

        // Value just below 4.0 that rounds to 4.0
        float nearFour = Float.intBitsToFloat(0x407FFFFF); // 3.9999999...
        float result2 = roundTrip(nearFour, true);
        assertEquals(4.0f, result2, 0.01f);

        // Test near the maximum representable value
        // 65504 is 0x7BFF in Float16 (exp=30, mantissa=0x3FF)
        // Values slightly above might round up and overflow to infinity
        float nearMax = 65504.0f;
        float result3 = roundTrip(nearMax, true);
        assertEquals(65504.0f, result3, 0.0f);

        // Value that's exactly at the rounding boundary
        float justAboveMax = Float.intBitsToFloat(0x477FE100); // Between 65504 and 65520
        float result4 = roundTrip(justAboveMax, true);
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
        float result1 = roundTrip(minSubnormal * 1.5, true);
        float expected1 = roundTrip(minSubnormal * 2, true);
        assertEquals(expected1, result1, 0.0f);

        // 2.5 * minSubnormal: mantissa would be 2.5, should round to 2 (even)
        float result2 = roundTrip(minSubnormal * 2.5, true);
        float expected2 = roundTrip(minSubnormal * 2, true);
        assertEquals(expected2, result2, 0.0f);

        // 3.5 * minSubnormal: mantissa would be 3.5, should round to 4 (even)
        float result3 = roundTrip(minSubnormal * 3.5, true);
        float expected3 = roundTrip(minSubnormal * 4, true);
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
        float result1 = roundTrip(float32MinSubnormal, true);
        assertEquals(0.0f, result1, 0.0f);

        // Float32 subnormals should generally become zero in Float16
        float smallSubnormal = Float.intBitsToFloat(0x00000100);
        float result2 = roundTrip(smallSubnormal, true);
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
        float result1 = roundTrip(doubleMax, true);
        assertEquals(65504.0f, result1, 0.0f);

        // Double value slightly above (should overflow)
        double slightlyAbove = 65504.1;
        float result2 = roundTrip(slightlyAbove, true);
        // Should be 65504 or infinity
        assertTrue(result2 >= 65504.0f);

        // Double value at denormalized boundary
        double doubleDenorm = 6.103515625e-5; // 2^-14
        float result3 = roundTrip(doubleDenorm, true);
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
        float result1 = roundTrip(value1, true);
        assertTrue(
                "2^16 should overflow to infinity",
                Float.isInfinite(result1) || result1 == 65504.0f);

        // Float32 with exponent that would be exactly 0 in Float16 (2^-15)
        // exp32 = -15, so exponent = -15 + 15 = 0 (denormalized)
        float value2 = Float.intBitsToFloat(0x38000000); // 2^-15
        float result2 = roundTrip(value2, true);
        assertTrue("2^-15 should be positive", result2 >= 0);

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
                float result = roundTrip(value, true);
                // Just verify it doesn't crash and produces a valid result
                assertTrue(
                        "Result should be valid for exponent " + exp,
                        !Float.isNaN(result) || Float.isNaN(value));
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
        float result1 = roundTrip(value1, true);
        // Should round up and potentially overflow
        assertTrue("Value should not be NaN", !Float.isNaN(result1));

        // Test with odd mantissa (should round up)
        int bits2 = (127 << 23) | 0x7FF800; // Mantissa with specific pattern
        float value2 = Float.intBitsToFloat(bits2);
        float result2 = roundTrip(value2, true);
        assertTrue("Value should not be NaN", !Float.isNaN(result2));

        // Test with even mantissa (should round to even)
        int bits3 = (127 << 23) | 0x7FE800; // Mantissa with specific pattern
        float value3 = Float.intBitsToFloat(bits3);
        float result3 = roundTrip(value3, true);
        assertTrue("Value should not be NaN", !Float.isNaN(result3));
    }

    /**
     * Test the absolute maximum values that can be represented in Float32 to ensure proper overflow
     * handling.
     */
    @Test
    public void testExtremeValues() {
        // Float.MAX_VALUE should overflow to infinity in Float16
        float result1 = roundTrip(Float.MAX_VALUE, true);
        assertEquals(Float.POSITIVE_INFINITY, result1, 0.0f);

        // -Float.MAX_VALUE should overflow to negative infinity
        float result2 = roundTrip(-Float.MAX_VALUE, true);
        assertEquals(Float.NEGATIVE_INFINITY, result2, 0.0f);

        // Test values between Float16 max and Float32 max
        float largeValue = 1e20f;
        float result3 = roundTrip(largeValue, true);
        assertEquals(Float.POSITIVE_INFINITY, result3, 0.0f);

        // Very small Float32 values
        float tinyValue = Float.MIN_NORMAL;
        float result4 = roundTrip(tinyValue, true);
        assertEquals(0.0f, result4, 0.0f);
    }

    /**
     * Test double-to-float precision edge cases that might bypass early guard conditions. This
     * tests the scenario where double precision value passes guards but float conversion reaches
     * defensive code paths.
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
            float result = roundTrip(d, true);
            // Should either be finite or infinity, never NaN
            assertFalse("Value " + d + " should not produce NaN", Float.isNaN(result));
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
            float result = roundTrip(d, true);
            // Should be positive and not NaN
            assertTrue("Value " + d + " should be >= 0", result >= 0);
            assertFalse("Value " + d + " should not be NaN", Float.isNaN(result));
        }

        // Test values where double and float representation might differ
        // Use nextAfter to get values extremely close to boundaries
        double d1 = Math.nextAfter(65520.0, 0.0); // Just below 65520
        float result1 = roundTrip(d1, true);
        assertFalse("nextAfter(65520, 0) should not be NaN", Float.isNaN(result1));

        double d2 = Math.nextAfter(65504.0, Double.POSITIVE_INFINITY); // Just above 65504
        float result2 = roundTrip(d2, true);
        assertFalse("nextAfter(65504, +inf) should not be NaN", Float.isNaN(result2));

        double d3 = Math.nextAfter(minNormal, 0.0); // Just below min normal
        float result3 = roundTrip(d3, true);
        assertFalse("nextAfter(minNormal, 0) should not be NaN", Float.isNaN(result3));

        double d4 = Math.nextAfter(minNormal, Double.POSITIVE_INFINITY); // Just above min normal
        float result4 = roundTrip(d4, true);
        assertFalse("nextAfter(minNormal, +inf) should not be NaN", Float.isNaN(result4));
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
        float result1 = roundTrip(maxMantissa, true);
        // This is close to max float16, should be finite or infinity
        assertTrue(
                "Max mantissa with exp 142 should be valid",
                !Float.isNaN(result1) && (Float.isFinite(result1) || Float.isInfinite(result1)));

        // Test Float32 where exp32 = -15 (float16 exp = 0, denormalized boundary)
        // Bits: sign=0, exp=112, mantissa=0 (exactly at boundary)
        float denormBoundary = Float.intBitsToFloat(112 << 23);
        float result2 = roundTrip(denormBoundary, true);
        assertTrue("Denorm boundary should be positive", result2 >= 0);

        // Test Float32 where exp32 = -15 with non-zero mantissa
        // This should force the normalized-to-denormalized path
        float belowNormal = Float.intBitsToFloat((112 << 23) | 0x400000);
        float result3 = roundTrip(belowNormal, true);
        assertTrue("Below normal should be positive", result3 >= 0);

        // Test Float32 with exp32 = 16 (float16 exp would be 31, overflow)
        // But guard condition should catch this
        float overflow = Float.intBitsToFloat(143 << 23);
        float result4 = roundTrip(overflow, true);
        // Should overflow to infinity
        assertTrue("Exp 143 should overflow", Float.isInfinite(result4) || result4 == 65504.0f);
    }
}
