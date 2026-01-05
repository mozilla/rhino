package org.mozilla.javascript.typedarrays;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests Float16 implementation against reference values from the IEEE 754 specification.
 *
 * <p>These test cases are based on the Float16 (binary16) format specification and verified against
 * standard implementations. Float16 uses:
 *
 * <ul>
 *   <li>1 sign bit
 *   <li>5 exponent bits (bias = 15)
 *   <li>10 mantissa bits
 * </ul>
 *
 * <p>Note: Node.js Float16 support requires v21+. These tests use reference values from the IEEE
 * 754 specification and can be verified with: new DataView(buffer).setFloat16(0, value)
 */
public class NodeJsFloat16ReferenceTest {

    private static class TestCase {
        final double input;
        final float expectedOutput;
        final int expectedBitsLE; // Little-endian bit pattern
        final boolean expectNaN;
        final boolean expectInfinite;

        TestCase(double input, float expected, int bitsLE, boolean isNaN, boolean isInf) {
            this.input = input;
            this.expectedOutput = expected;
            this.expectedBitsLE = bitsLE;
            this.expectNaN = isNaN;
            this.expectInfinite = isInf;
        }
    }

    // Reference test cases based on IEEE 754 Float16 specification
    private static final TestCase[] REFERENCE_CASES = {
        // Zero
        new TestCase(0.0, 0.0f, 0x0000, false, false),
        new TestCase(-0.0, -0.0f, 0x8000, false, false),

        // One and simple powers of 2
        new TestCase(1.0, 1.0f, 0x3C00, false, false), // sign=0, exp=15, mant=0
        new TestCase(-1.0, -1.0f, 0xBC00, false, false),
        new TestCase(2.0, 2.0f, 0x4000, false, false), // sign=0, exp=16, mant=0
        new TestCase(4.0, 4.0f, 0x4400, false, false),
        new TestCase(0.5, 0.5f, 0x3800, false, false), // sign=0, exp=14, mant=0

        // Infinity
        new TestCase(Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, 0x7C00, false, true),
        new TestCase(Double.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, 0xFC00, false, true),

        // NaN (0x7E00 is a canonical NaN pattern for Float16)
        new TestCase(Double.NaN, Float.NaN, 0x7E00, true, false),

        // Max Float16 value (65504 = (2^16 - 2^6) = 0x7BFF)
        new TestCase(65504.0, 65504.0f, 0x7BFF, false, false),
        new TestCase(-65504.0, -65504.0f, 0xFBFF, false, false),

        // Values between max finite and overflow threshold
        // ULP at exp=30 is 32, so halfway to next representable is 65504+16=65520
        // Values < 65520 round to 65504, values >= 65520 overflow to infinity
        new TestCase(65505.0, 65504.0f, 0x7BFF, false, false), // rounds to max finite

        // Overflow to infinity
        new TestCase(
                65520.0,
                Float.POSITIVE_INFINITY,
                0x7C00,
                false,
                true), // exactly halfway, rounds to even (infinity has even mantissa 0)
        new TestCase(65521.0, Float.POSITIVE_INFINITY, 0x7C00, false, true), // > halfway
        new TestCase(100000.0, Float.POSITIVE_INFINITY, 0x7C00, false, true),
        new TestCase(-100000.0, Float.NEGATIVE_INFINITY, 0xFC00, false, true),

        // Min positive normal: 2^-14 = 0.00006103515625
        // Binary: sign=0, exp=1, mant=0 => 0x0400
        new TestCase(0.00006103515625, 0.00006103515625f, 0x0400, false, false),

        // Just below min normal (denormalized)
        // 2^-15 = 0.000030517578125
        // Binary: sign=0, exp=0, mant=512 => 0x0200
        new TestCase(0.000030517578125, 0.000030517578125f, 0x0200, false, false),

        // Min positive subnormal: 2^-24 ≈ 5.960464477539063e-8
        // Binary: sign=0, exp=0, mant=1 => 0x0001
        new TestCase(5.960464477539063e-8, 5.960464477539063e-8f, 0x0001, false, false),

        // Common fractions
        new TestCase(0.25, 0.25f, 0x3400, false, false),
        new TestCase(0.75, 0.75f, 0x3A00, false, false),
        new TestCase(1.5, 1.5f, 0x3E00, false, false),

        // Small denormalized values
        // 1e-7 is closer to 2*2^-24 than to 1*2^-24, so it rounds to 0x0002
        new TestCase(1e-7, 1.1920928955078125e-7f, 0x0002, false, false),
        // 2e-7 is closer to 3*2^-24
        new TestCase(2e-7, 1.788139e-7f, 0x0003, false, false),

        // Test rounding
        new TestCase(1.0009765625, 1.0009765625f, 0x3C01, false, false), // 1 + 2^-10
        new TestCase(1.0, 1.0f, 0x3C00, false, false),
    };

    @Test
    public void testAgainstReferenceValues() {
        for (TestCase tc : REFERENCE_CASES) {
            byte[] buf = new byte[2];
            ByteIo.writeFloat16(buf, 0, tc.input, true);

            // Check bit pattern
            int actualBits = (buf[0] & 0xff) | ((buf[1] & 0xff) << 8);
            assertEquals(
                    String.format(
                            "Bit pattern mismatch for input %.15e (%.10f)", tc.input, tc.input),
                    tc.expectedBitsLE,
                    actualBits);

            // Check read-back value
            float result = ByteIo.readFloat16(buf, 0, true);

            if (tc.expectNaN) {
                assertTrue("Expected NaN for input " + tc.input, Float.isNaN(result));
            } else if (tc.expectInfinite) {
                assertTrue("Expected infinite for input " + tc.input, Float.isInfinite(result));
                assertEquals(
                        "Infinity sign mismatch for input " + tc.input,
                        Math.signum(tc.expectedOutput),
                        Math.signum(result),
                        0.0f);
            } else {
                assertEquals(
                        String.format("Value mismatch for input %.15e", tc.input),
                        tc.expectedOutput,
                        result,
                        Math.abs(tc.expectedOutput) * 1e-6f);
            }
        }
    }

    @Test
    public void testBigEndianReferenceValues() {
        for (TestCase tc : REFERENCE_CASES) {
            byte[] buf = new byte[2];
            ByteIo.writeFloat16(buf, 0, tc.input, false);

            // Check bit pattern (big-endian)
            int actualBits = ((buf[0] & 0xff) << 8) | (buf[1] & 0xff);
            assertEquals(
                    String.format("Big-endian bit pattern mismatch for input %.15e", tc.input),
                    tc.expectedBitsLE,
                    actualBits);

            // Check read-back value
            float result = ByteIo.readFloat16(buf, 0, false);

            if (tc.expectNaN) {
                assertTrue(Float.isNaN(result));
            } else if (tc.expectInfinite) {
                assertTrue(Float.isInfinite(result));
                assertEquals(Math.signum(tc.expectedOutput), Math.signum(result), 0.0f);
            } else {
                assertEquals(tc.expectedOutput, result, Math.abs(tc.expectedOutput) * 1e-6f);
            }
        }
    }

    @Test
    public void testSpecificBitPatterns() {
        // Test some known bit patterns directly

        // 0x3C00 = 0011110000000000 (binary)
        // sign=0, exp=01111=15, mant=0000000000 => 1.0 * 2^(15-15) = 1.0
        assertBitPatternEquals(0x3C00, 1.0, true);

        // 0x4000 = 0100000000000000 (binary)
        // sign=0, exp=10000=16, mant=0000000000 => 1.0 * 2^(16-15) = 2.0
        assertBitPatternEquals(0x4000, 2.0, true);

        // 0x3800 = 0011100000000000 (binary)
        // sign=0, exp=01110=14, mant=0000000000 => 1.0 * 2^(14-15) = 0.5
        assertBitPatternEquals(0x3800, 0.5, true);

        // 0x7C00 = 0111110000000000 (binary)
        // sign=0, exp=11111=31, mant=0000000000 => +Infinity
        assertBitPatternEquals(0x7C00, Float.POSITIVE_INFINITY, true);

        // 0xFC00 = 1111110000000000 (binary)
        // sign=1, exp=11111=31, mant=0000000000 => -Infinity
        assertBitPatternEquals(0xFC00, Float.NEGATIVE_INFINITY, true);
    }

    private void assertBitPatternEquals(int expectedBits, double input, boolean littleEndian) {
        byte[] buf = new byte[2];
        ByteIo.writeFloat16(buf, 0, input, littleEndian);

        int actualBits;
        if (littleEndian) {
            actualBits = (buf[0] & 0xff) | ((buf[1] & 0xff) << 8);
        } else {
            actualBits = ((buf[0] & 0xff) << 8) | (buf[1] & 0xff);
        }

        assertEquals(
                String.format(
                        "Expected bits 0x%04X for input %f, got 0x%04X",
                        expectedBits, input, actualBits),
                expectedBits,
                actualBits);
    }

    @Test
    public void testDenormalizedBitPatterns() {
        // Denormalized numbers have exponent = 0

        // 0x0001: smallest positive denormalized (2^-24)
        byte[] buf = new byte[2];
        buf[0] = 0x01;
        buf[1] = 0x00;
        float result = ByteIo.readFloat16(buf, 0, true);
        assertEquals(5.960464477539063e-8f, result, 1e-15f);

        // 0x0002: 2 * 2^-24
        buf[0] = 0x02;
        buf[1] = 0x00;
        result = ByteIo.readFloat16(buf, 0, true);
        assertEquals(1.1920928955078125e-7f, result, 1e-15f);

        // 0x03FF: largest denormalized
        buf[0] = (byte) 0xFF;
        buf[1] = 0x03;
        result = ByteIo.readFloat16(buf, 0, true);
        // This is (1023 / 1024) * 2^-14
        assertTrue(result > 0);
        assertTrue(result < 0.00006103515625); // Less than min normal
    }

    @Test
    public void testRoundingToNearest() {
        // Test that values round to nearest representable Float16
        // Float16 has 10-bit mantissa, so smallest representable increment from 1.0 is 2^-10

        // 1.0009765625 = 1 + 2^-10, exactly representable (smallest step from 1.0)
        assertRoundsTo(1.0009765625, 1.0009765625f);

        // 1.00048828125 = 1 + 2^-11, exactly halfway between 1.0 and 1 + 2^-10
        // IEEE 754 round-to-nearest-even: round to 1.0 (mantissa 0 is even)
        assertRoundsTo(1.00048828125, 1.0f);

        // Values between representable numbers should round to nearest
        // Test value very close to 1.0
        double almostOne = 1.0 + 1e-10;
        float result = roundTrip(almostOne);
        assertEquals(1.0f, result, 0.0f);
    }

    private void assertRoundsTo(double input, float expected) {
        float result = roundTrip(input);
        assertEquals(
                String.format("Rounding failed for %.15e", input),
                expected,
                result,
                Math.abs(expected) * 1e-6f);
    }

    private float roundTrip(double value) {
        byte[] buf = new byte[2];
        ByteIo.writeFloat16(buf, 0, value, true);
        return ByteIo.readFloat16(buf, 0, true);
    }
}
