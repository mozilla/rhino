/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/**
 * **************************************************************
 *
 * <p>The author of this software is David M. Gay.
 *
 * <p>Copyright (c) 1991, 2000, 2001 by Lucent Technologies.
 *
 * <p>Permission to use, copy, modify, and distribute this software for any purpose without fee is
 * hereby granted, provided that this entire notice is included in all copies of any software which
 * is or includes a copy or modification of this software and in all copies of the supporting
 * documentation for such software.
 *
 * <p>THIS SOFTWARE IS BEING PROVIDED "AS IS", WITHOUT ANY EXPRESS OR IMPLIED WARRANTY. IN
 * PARTICULAR, NEITHER THE AUTHOR NOR LUCENT MAKES ANY REPRESENTATION OR WARRANTY OF ANY KIND
 * CONCERNING THE MERCHANTABILITY OF THIS SOFTWARE OR ITS FITNESS FOR ANY PARTICULAR PURPOSE.
 *
 * <p>*************************************************************
 */
package org.mozilla.javascript;

import java.math.BigInteger;
import java.util.Objects;

class DToA {

    private static char BASEDIGIT(int digit) {
        return (char) ((digit >= 10) ? 'a' - 10 + digit : '0' + digit);
    }

    private static final int Frac_mask = 0xfffff;
    private static final int Exp_shift = 20;
    private static final int Exp_msk1 = 0x100000;

    private static final long Frac_maskL = 0xfffffffffffffL;
    private static final int Exp_shiftL = 52;
    private static final long Exp_msk1L = 0x10000000000000L;

    private static final int Bias = 1023;
    private static final int P = 53;

    private static final int Exp_shift1 = 20;
    private static final int Exp_mask = 0x7ff00000;
    private static final int Exp_mask_shifted = 0x7ff;
    private static final int Bndry_mask = 0xfffff;
    private static final int Log2P = 1;

    private static final int Sign_bit = 0x80000000;
    private static final int Exp_11 = 0x3ff00000;
    private static final int Ten_pmax = 22;
    private static final int Quick_max = 14;
    private static final int Bletch = 0x10;
    private static final int Frac_mask1 = 0xfffff;
    private static final int Int_max = 14;
    private static final int n_bigtens = 5;

    private static final double[] tens = {
        1e0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9,
        1e10, 1e11, 1e12, 1e13, 1e14, 1e15, 1e16, 1e17, 1e18, 1e19,
        1e20, 1e21, 1e22
    };

    private static final double[] bigtens = {1e16, 1e32, 1e64, 1e128, 1e256};

    private static int lo0bits(int y) {
        int k;
        int x = y;

        if ((x & 7) != 0) {
            if ((x & 1) != 0) return 0;
            if ((x & 2) != 0) {
                return 1;
            }
            return 2;
        }
        k = 0;
        if ((x & 0xffff) == 0) {
            k = 16;
            x >>>= 16;
        }
        if ((x & 0xff) == 0) {
            k += 8;
            x >>>= 8;
        }
        if ((x & 0xf) == 0) {
            k += 4;
            x >>>= 4;
        }
        if ((x & 0x3) == 0) {
            k += 2;
            x >>>= 2;
        }
        if ((x & 1) == 0) {
            k++;
            x >>>= 1;
            if ((x & 1) == 0) return 32;
        }
        return k;
    }

    /* Return the number (0 through 32) of most significant zero bits in x. */
    private static int hi0bits(int x) {
        int k = 0;

        if ((x & 0xffff0000) == 0) {
            k = 16;
            x <<= 16;
        }
        if ((x & 0xff000000) == 0) {
            k += 8;
            x <<= 8;
        }
        if ((x & 0xf0000000) == 0) {
            k += 4;
            x <<= 4;
        }
        if ((x & 0xc0000000) == 0) {
            k += 2;
            x <<= 2;
        }
        if ((x & 0x80000000) == 0) {
            k++;
            if ((x & 0x40000000) == 0) return 32;
        }
        return k;
    }

    private static void stuffBits(byte[] bits, int offset, int val) {
        bits[offset] = (byte) (val >> 24);
        bits[offset + 1] = (byte) (val >> 16);
        bits[offset + 2] = (byte) (val >> 8);
        bits[offset + 3] = (byte) val;
    }

    /* Convert d into the form b*2^e, where b is an odd integer.  b is the returned
     * Bigint and e is the returned binary exponent.  Return the number of significant
     * bits in b in bits.  d must be finite and nonzero. */
    private static BigInteger d2b(double d, int[] e, int[] bits) {
        byte[] dbl_bits;
        int i, k, y, z, de;
        long dBits = Double.doubleToLongBits(d);
        int d0 = (int) (dBits >>> 32);
        int d1 = (int) dBits;

        z = d0 & Frac_mask;
        d0 &= 0x7fffffff; /* clear sign bit, which we ignore */

        if ((de = (d0 >>> Exp_shift)) != 0) z |= Exp_msk1;

        if ((y = d1) != 0) {
            dbl_bits = new byte[8];
            k = lo0bits(y);
            y >>>= k;
            if (k != 0) {
                stuffBits(dbl_bits, 4, y | z << (32 - k));
                z >>= k;
            } else stuffBits(dbl_bits, 4, y);
            stuffBits(dbl_bits, 0, z);
            i = (z != 0) ? 2 : 1;
        } else {
            //        JS_ASSERT(z);
            dbl_bits = new byte[4];
            k = lo0bits(z);
            z >>>= k;
            stuffBits(dbl_bits, 0, z);
            k += 32;
            i = 1;
        }
        if (de != 0) {
            e[0] = de - Bias - (P - 1) + k;
            bits[0] = P - k;
        } else {
            e[0] = de - Bias - (P - 1) + 1 + k;
            bits[0] = 32 * i - hi0bits(z);
        }
        return new BigInteger(dbl_bits);
    }

    static String JS_dtobasestr(int base, double d) {
        if (!(2 <= base && base <= 36)) throw new IllegalArgumentException("Bad base: " + base);

        /* Check for Infinity and NaN */
        if (Double.isNaN(d)) {
            return "NaN";
        } else if (Double.isInfinite(d)) {
            return (d > 0.0) ? "Infinity" : "-Infinity";
        } else if (d == 0) {
            // ALERT: should it distinguish -0.0 from +0.0 ?
            return "0";
        }

        boolean negative;
        if (d >= 0.0) {
            negative = false;
        } else {
            negative = true;
            d = -d;
        }

        /* Get the integer part of d including '-' sign. */
        String intDigits;

        double dfloor = Math.floor(d);
        long lfloor = (long) dfloor;
        if (lfloor == dfloor) {
            // int part fits long
            intDigits = Long.toString(negative ? -lfloor : lfloor, base);
        } else {
            // BigInteger should be used
            long floorBits = Double.doubleToLongBits(dfloor);
            int exp = (int) (floorBits >> Exp_shiftL) & Exp_mask_shifted;
            long mantissa;
            if (exp == 0) {
                mantissa = (floorBits & Frac_maskL) << 1;
            } else {
                mantissa = (floorBits & Frac_maskL) | Exp_msk1L;
            }
            if (negative) {
                mantissa = -mantissa;
            }
            exp -= 1075;
            BigInteger x = BigInteger.valueOf(mantissa);
            if (exp > 0) {
                x = x.shiftLeft(exp);
            } else if (exp < 0) {
                x = x.shiftRight(-exp);
            }
            intDigits = x.toString(base);
        }

        if (d == dfloor) {
            // No fraction part
            return intDigits;
        }
        /* We have a fraction. */

        StringBuilder buffer; /* The output string */
        int digit;
        double df; /* The fractional part of d */
        BigInteger b;

        buffer = new StringBuilder();
        buffer.append(intDigits).append('.');
        df = d - dfloor;

        long dBits = Double.doubleToLongBits(d);
        int word0 = (int) (dBits >> 32);
        int word1 = (int) dBits;

        int[] e = new int[1];
        int[] bbits = new int[1];

        b = d2b(df, e, bbits);
        //            JS_ASSERT(e < 0);
        /* At this point df = b * 2^e.  e must be less than zero because 0 < df < 1. */

        int s2 = -(word0 >>> Exp_shift1 & Exp_mask >> Exp_shift1);
        if (s2 == 0) s2 = -1;
        s2 += Bias + P;
        /* 1/2^s2 = (nextDouble(d) - d)/2 */
        //            JS_ASSERT(-s2 < e);
        BigInteger mlo = BigInteger.valueOf(1);
        BigInteger mhi = mlo;
        if ((word1 == 0)
                && ((word0 & Bndry_mask) == 0)
                && ((word0 & (Exp_mask & Exp_mask << 1)) != 0)) {
            /* The special case.  Here we want to be within a quarter of the last input
            significant digit instead of one half of it when the output string's value is less than d.  */
            s2 += Log2P;
            mhi = BigInteger.valueOf(1 << Log2P);
        }

        b = b.shiftLeft(e[0] + s2);
        BigInteger s = BigInteger.valueOf(1);
        s = s.shiftLeft(s2);
        /* At this point we have the following:
         *   s = 2^s2;
         *   1 > df = b/2^s2 > 0;
         *   (d - prevDouble(d))/2 = mlo/2^s2;
         *   (nextDouble(d) - d)/2 = mhi/2^s2. */
        BigInteger bigBase = BigInteger.valueOf(base);

        boolean done = false;
        do {
            b = b.multiply(bigBase);
            BigInteger[] divResult = b.divideAndRemainder(s);
            b = divResult[1];
            digit = (char) divResult[0].intValue();
            if (Objects.equals(mlo, mhi)) mlo = mhi = mlo.multiply(bigBase);
            else {
                mlo = mlo.multiply(bigBase);
                mhi = mhi.multiply(bigBase);
            }

            /* Do we yet have the shortest string that will round to d? */
            int j = b.compareTo(mlo);
            /* j is b/2^s2 compared with mlo/2^s2. */
            BigInteger delta = s.subtract(mhi);
            int j1 = (delta.signum() <= 0) ? 1 : b.compareTo(delta);
            /* j1 is b/2^s2 compared with 1 - mhi/2^s2. */
            if (j1 == 0 && ((word1 & 1) == 0)) {
                if (j > 0) digit++;
                done = true;
            } else if (j < 0 || (j == 0 && ((word1 & 1) == 0))) {
                if (j1 > 0) {
                    /* Either dig or dig+1 would work here as the least significant digit.
                    Use whichever would produce an output value closer to d. */
                    b = b.shiftLeft(1);
                    j1 = b.compareTo(s);
                    if (j1
                            > 0) /* The even test (|| (j1 == 0 && (digit & 1))) is not here because it messes up odd base output
                                  * such as 3.5 in base 3.  */
                        digit++;
                }
                done = true;
            } else if (j1 > 0) {
                digit++;
                done = true;
            }
            //                JS_ASSERT(digit < (uint32)base);
            buffer.append(BASEDIGIT(digit));
        } while (!done);

        return buffer.toString();
    }
}
