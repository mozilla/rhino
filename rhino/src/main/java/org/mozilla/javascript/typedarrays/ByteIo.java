/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

public class ByteIo {

    // Float16 constants
    private static final double FLOAT16_MIN_NORMAL = 6.103515625e-5; // 2^-14
    private static final double FLOAT16_MIN_SUBNORMAL = 5.9604644775390625E-8; // 2^-24

    public static Byte readInt8(byte[] buf, int offset) {
        return Byte.valueOf(buf[offset]);
    }

    public static void writeInt8(byte[] buf, int offset, int val) {
        buf[offset] = (byte) val;
    }

    public static Integer readUint8(byte[] buf, int offset) {
        return Integer.valueOf(buf[offset] & 0xff);
    }

    public static void writeUint8(byte[] buf, int offset, int val) {
        buf[offset] = (byte) (val & 0xff);
    }

    private static short doReadInt16(byte[] buf, int offset, boolean littleEndian) {
        // Need to coalesce to short here so that we stay in range
        if (littleEndian) {
            return (short) ((buf[offset] & 0xff) | ((buf[offset + 1] & 0xff) << 8));
        }
        return (short) (((buf[offset] & 0xff) << 8) | (buf[offset + 1] & 0xff));
    }

    private static void doWriteInt16(byte[] buf, int offset, int val, boolean littleEndian) {
        if (littleEndian) {
            buf[offset] = (byte) (val & 0xff);
            buf[offset + 1] = (byte) ((val >>> 8) & 0xff);
        } else {
            buf[offset] = (byte) ((val >>> 8) & 0xff);
            buf[offset + 1] = (byte) (val & 0xff);
        }
    }

    public static Short readInt16(byte[] buf, int offset, boolean littleEndian) {
        return Short.valueOf(doReadInt16(buf, offset, littleEndian));
    }

    public static void writeInt16(byte[] buf, int offset, int val, boolean littleEndian) {
        doWriteInt16(buf, offset, val, littleEndian);
    }

    public static Integer readUint16(byte[] buf, int offset, boolean littleEndian) {
        return Integer.valueOf(doReadInt16(buf, offset, littleEndian) & 0xffff);
    }

    public static void writeUint16(byte[] buf, int offset, int val, boolean littleEndian) {
        doWriteInt16(buf, offset, val & 0xffff, littleEndian);
    }

    public static Integer readInt32(byte[] buf, int offset, boolean littleEndian) {
        if (littleEndian) {
            return Integer.valueOf(
                    (buf[offset] & 0xff)
                            | ((buf[offset + 1] & 0xff) << 8)
                            | ((buf[offset + 2] & 0xff) << 16)
                            | ((buf[offset + 3] & 0xff) << 24));
        }
        return Integer.valueOf(
                ((buf[offset] & 0xff) << 24)
                        | ((buf[offset + 1] & 0xff) << 16)
                        | ((buf[offset + 2] & 0xff) << 8)
                        | (buf[offset + 3] & 0xff));
    }

    public static void writeInt32(byte[] buf, int offset, int val, boolean littleEndian) {
        if (littleEndian) {
            buf[offset] = (byte) (val & 0xff);
            buf[offset + 1] = (byte) ((val >>> 8) & 0xff);
            buf[offset + 2] = (byte) ((val >>> 16) & 0xff);
            buf[offset + 3] = (byte) ((val >>> 24) & 0xff);
        } else {
            buf[offset] = (byte) ((val >>> 24) & 0xff);
            buf[offset + 1] = (byte) ((val >>> 16) & 0xff);
            buf[offset + 2] = (byte) ((val >>> 8) & 0xff);
            buf[offset + 3] = (byte) (val & 0xff);
        }
    }

    public static long readUint32Primitive(byte[] buf, int offset, boolean littleEndian) {
        if (littleEndian) {
            return ((buf[offset] & 0xffL)
                            | ((buf[offset + 1] & 0xffL) << 8L)
                            | ((buf[offset + 2] & 0xffL) << 16L)
                            | ((buf[offset + 3] & 0xffL) << 24L))
                    & 0xffffffffL;
        }
        return (((buf[offset] & 0xffL) << 24L)
                        | ((buf[offset + 1] & 0xffL) << 16L)
                        | ((buf[offset + 2] & 0xffL) << 8L)
                        | (buf[offset + 3] & 0xffL))
                & 0xffffffffL;
    }

    public static void writeUint32(byte[] buf, int offset, long val, boolean littleEndian) {
        if (littleEndian) {
            buf[offset] = (byte) (val & 0xffL);
            buf[offset + 1] = (byte) ((val >>> 8L) & 0xffL);
            buf[offset + 2] = (byte) ((val >>> 16L) & 0xffL);
            buf[offset + 3] = (byte) ((val >>> 24L) & 0xffL);
        } else {
            buf[offset] = (byte) ((val >>> 24L) & 0xffL);
            buf[offset + 1] = (byte) ((val >>> 16L) & 0xffL);
            buf[offset + 2] = (byte) ((val >>> 8L) & 0xffL);
            buf[offset + 3] = (byte) (val & 0xffL);
        }
    }

    public static Object readUint32(byte[] buf, int offset, boolean littleEndian) {
        return Long.valueOf(readUint32Primitive(buf, offset, littleEndian));
    }

    public static long readUint64Primitive(byte[] buf, int offset, boolean littleEndian) {
        if (littleEndian) {
            return ((buf[offset] & 0xffL)
                    | ((buf[offset + 1] & 0xffL) << 8L)
                    | ((buf[offset + 2] & 0xffL) << 16L)
                    | ((buf[offset + 3] & 0xffL) << 24L)
                    | ((buf[offset + 4] & 0xffL) << 32L)
                    | ((buf[offset + 5] & 0xffL) << 40L)
                    | ((buf[offset + 6] & 0xffL) << 48L)
                    | ((buf[offset + 7] & 0xffL) << 56L));
        }
        return (((buf[offset] & 0xffL) << 56L)
                | ((buf[offset + 1] & 0xffL) << 48L)
                | ((buf[offset + 2] & 0xffL) << 40L)
                | ((buf[offset + 3] & 0xffL) << 32L)
                | ((buf[offset + 4] & 0xffL) << 24L)
                | ((buf[offset + 5] & 0xffL) << 16L)
                | ((buf[offset + 6] & 0xffL) << 8L)
                | ((buf[offset + 7] & 0xffL) << 0L));
    }

    public static void writeUint64(byte[] buf, int offset, long val, boolean littleEndian) {
        if (littleEndian) {
            buf[offset] = (byte) (val & 0xffL);
            buf[offset + 1] = (byte) ((val >>> 8L) & 0xffL);
            buf[offset + 2] = (byte) ((val >>> 16L) & 0xffL);
            buf[offset + 3] = (byte) ((val >>> 24L) & 0xffL);
            buf[offset + 4] = (byte) ((val >>> 32L) & 0xffL);
            buf[offset + 5] = (byte) ((val >>> 40L) & 0xffL);
            buf[offset + 6] = (byte) ((val >>> 48L) & 0xffL);
            buf[offset + 7] = (byte) ((val >>> 56L) & 0xffL);
        } else {
            buf[offset] = (byte) ((val >>> 56L) & 0xffL);
            buf[offset + 1] = (byte) ((val >>> 48L) & 0xffL);
            buf[offset + 2] = (byte) ((val >>> 40L) & 0xffL);
            buf[offset + 3] = (byte) ((val >>> 32L) & 0xffL);
            buf[offset + 4] = (byte) ((val >>> 24L) & 0xffL);
            buf[offset + 5] = (byte) ((val >>> 16L) & 0xffL);
            buf[offset + 6] = (byte) ((val >>> 8L) & 0xffL);
            buf[offset + 7] = (byte) (val & 0xffL);
        }
    }

    public static Float readFloat16(byte[] buf, int offset, boolean littleEndian) {
        int bits = doReadInt16(buf, offset, littleEndian) & 0xffff;

        // Extract sign, exponent, and mantissa
        int sign = (bits >>> 15) & 0x1;
        int exponent = (bits >>> 10) & 0x1f;
        int mantissa = bits & 0x3ff;

        // Handle special cases
        if (exponent == 0) {
            if (mantissa == 0) {

                // Zero
                return sign == 0 ? 0.0f : -0.0f;
            }

            // Denormalized number
            float value = (float) ((double) mantissa / (1 << 10) * FLOAT16_MIN_NORMAL);
            return sign == 0 ? value : -value;

        } else if (exponent == 31) {
            if (mantissa == 0) {

                // Infinity
                return sign == 0 ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
            }

            // NaN
            return Float.NaN;

        } else {

            // Normalized number
            float value =
                    (float) ((1.0 + (double) mantissa / (1 << 10)) * Math.pow(2, exponent - 15));
            return sign == 0 ? value : -value;
        }
    }

    public static void writeFloat16(byte[] buf, int offset, double val, boolean littleEndian) {
        float fval = (float) val;

        // Handle special cases
        if (Float.isNaN(fval)) {
            doWriteInt16(buf, offset, 0x7e00, littleEndian);
            return;
        }

        int sign = (Float.floatToIntBits(fval) >>> 31) & 0x1;
        float absVal = Math.abs(fval);

        if (Float.isInfinite(fval)) {

            // Infinity
            doWriteInt16(buf, offset, (sign << 15) | 0x7c00, littleEndian);
            return;
        }

        if (absVal == 0.0f) {

            // Zero
            doWriteInt16(buf, offset, sign << 15, littleEndian);
            return;
        }

        // Convert to float16
        int exponent;
        int mantissa;

        // Check overflow using original double precision
        // Max representable float16 is 65504 (exp=30, mantissa=0x3ff)
        // Values >= 65520 definitely overflow to infinity
        // Values in [65504, 65520) might round to 65504 or infinity
        double absValDouble = Math.abs(val);
        if (absValDouble >= 65520.0) {

            // Definite overflow to infinity
            doWriteInt16(buf, offset, (sign << 15) | 0x7c00, littleEndian);
            return;
        } else if (absValDouble > 65504.0) {

            // Near overflow: value is between max finite and definite overflow
            // These values might round to 65504 or infinity depending on exact value
            // 65504 = 0x7BFF: exp=30, mantissa=0x3ff (all 1s)
            // Halfway point to next value would cause overflow
            // Values < 65520 should round to 65504
            doWriteInt16(buf, offset, (sign << 15) | 0x7BFF, littleEndian);
            return;
        }
        if (absVal < (float) FLOAT16_MIN_NORMAL) {

            // Denormalized number - IEEE 754 round-to-nearest-even
            // Use original double precision for accuracy in denormalized range
            exponent = 0;

            double ratio = Math.abs(val) / FLOAT16_MIN_SUBNORMAL;
            int mantissaBase = (int) ratio;
            double fractional = ratio - mantissaBase;

            // IEEE 754 round-to-nearest-even
            if (fractional > 0.5) {
                // More than halfway: round up
                mantissa = mantissaBase + 1;
            } else if (fractional == 0.5) {
                // Tie: round to even
                if ((mantissaBase & 1) != 0) {
                    mantissa = mantissaBase + 1; // Round up if odd
                } else {
                    mantissa = mantissaBase; // Keep if even
                }
            } else {
                // Less than halfway: round down
                mantissa = mantissaBase;
            }
        } else {

            // Normalized
            int exp32 = ((Float.floatToIntBits(fval) >>> 23) & 0xff) - 127;
            exponent = exp32 + 15;

            if (exponent >= 31) {

                // Overflow to infinity
                doWriteInt16(buf, offset, (sign << 15) | 0x7c00, littleEndian);
                return;
            }
            if (exponent <= 0) {

                // Denormalized - IEEE 754 round-to-nearest-even
                exponent = 0;

                double ratio = (absVal / FLOAT16_MIN_NORMAL) * (1 << 10);
                int mantissaBase = (int) ratio;
                double fractional = ratio - mantissaBase;

                // IEEE 754 round-to-nearest-even
                if (fractional > 0.5) {
                    // More than halfway: round up
                    mantissa = mantissaBase + 1;
                } else if (fractional == 0.5) {
                    // Tie: round to even
                    if ((mantissaBase & 1) != 0) {
                        mantissa = mantissaBase + 1; // Round up if odd
                    } else {
                        mantissa = mantissaBase; // Keep if even
                    }
                } else {
                    // Less than halfway: round down
                    mantissa = mantissaBase;
                }
                mantissa &= 0x3ff;
            } else {

                // Normalized
                int mant32 = Float.floatToIntBits(fval) & 0x7fffff;

                // Implement IEEE 754 round-to-nearest-even
                int bitsToShift = mant32 & 0x1fff; // Bits [12:0] that will be lost
                mantissa = mant32 >>> 13; // Bits [22:13] become the new mantissa

                if (bitsToShift > 0x1000) {
                    // More than halfway: round up
                    mantissa++;
                } else if (bitsToShift == 0x1000) {
                    // Exactly halfway: round to even (check LSB)
                    if ((mantissa & 1) != 0) {
                        mantissa++; // Round up if odd
                    }
                }
                // else: Less than halfway, round down (mantissa already truncated)

                // Handle rounding overflow
                if (mantissa >= 0x400) {
                    exponent++;
                    mantissa = 0;
                    if (exponent >= 31) {

                        // Overflow to infinity
                        doWriteInt16(buf, offset, (sign << 15) | 0x7c00, littleEndian);
                        return;
                    }
                }
            }
        }

        int bits = (sign << 15) | (exponent << 10) | mantissa;
        doWriteInt16(buf, offset, bits, littleEndian);
    }

    public static Float readFloat32(byte[] buf, int offset, boolean littleEndian) {
        long base = readUint32Primitive(buf, offset, littleEndian);
        return Float.valueOf(Float.intBitsToFloat((int) base));
    }

    public static void writeFloat32(byte[] buf, int offset, double val, boolean littleEndian) {
        long base = Float.floatToIntBits((float) val);
        writeUint32(buf, offset, base, littleEndian);
    }

    public static Double readFloat64(byte[] buf, int offset, boolean littleEndian) {
        long base = readUint64Primitive(buf, offset, littleEndian);
        return Double.valueOf(Double.longBitsToDouble(base));
    }

    public static void writeFloat64(byte[] buf, int offset, double val, boolean littleEndian) {
        long base = Double.doubleToLongBits(val);
        writeUint64(buf, offset, base, littleEndian);
    }
}
