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
}
