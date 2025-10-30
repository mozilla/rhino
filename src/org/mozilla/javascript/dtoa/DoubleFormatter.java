package org.mozilla.javascript.dtoa;

/**
 * This class formats a double into a set of digits and an exponent for further formatting by the
 * Double class. It is based on the same code used by Jackson and in OpenJDK with the following
 * copyright:
 *
 * <p>Copyright 2018-2020 Raffaello Giulietti
 *
 * <p>Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * <p>The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * <p>THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
public class DoubleFormatter {
    /*
    For full details about this code see the following references:

    [1] Giulietti, "The Schubfach way to render doubles",
        https://drive.google.com/open?id=1luHhyQF9zKlM8yJ1nebU0OgVYhfC6CBN

    [2] IEEE Computer Society, "IEEE Standard for Floating-Point Arithmetic"

    [3] Bouvier & Zimmermann, "Division-Free Binary-to-Decimal Conversion"

    Divisions are avoided altogether for the benefit of those architectures
    that do not provide specific machine instructions or where they are slow.
    This is discussed in section 10 of [1].
     */

    // Sources with the license are here:
    // https://github.com/c4f7fcce9cb06515/Schubfach/blob/3c92d3c9b1fead540616c918cdfef432bca53dfa/todec/src/math/FloatToDecimal.java

    // The precision in bits.
    static final int P = 53;

    // Exponent width in bits.
    private static final int W = (Double.SIZE - 1) - (P - 1);

    // Minimum value of the exponent: -(2^(W-1)) - P + 3.
    static final int Q_MIN = (-1 << (W - 1)) - P + 3;

    // Maximum value of the exponent: 2^(W-1) - P.
    static final int Q_MAX = (1 << (W - 1)) - P;

    // 10^(E_MIN - 1) <= MIN_VALUE < 10^E_MIN
    static final int E_MIN = -323;

    // 10^(E_MAX - 1) <= MAX_VALUE < 10^E_MAX
    static final int E_MAX = 309;

    // Threshold to detect tiny values, as in section 8.1.1 of [1]
    static final long C_TINY = 3;

    // H is as in section 8 of [1].
    static final int H = 17;

    // Minimum value of the significand of a normal value: 2^(P-1).
    private static final long C_MIN = 1L << (P - 1);

    // Mask to extract the biased exponent.
    private static final int BQ_MASK = (1 << W) - 1;

    // Mask to extract the fraction bits.
    private static final long T_MASK = (1L << (P - 1)) - 1;

    // Used in rop().
    private static final long MASK_63 = (1L << 63) - 1;

    /**
     * Convert a double to String as defined in the "Number::toString" operation in ECMAScript. It
     * can handle any number including non-finite numbers.
     */
    public static String toString(double v) {
        /*
        For full details see references [2] and [1].

        For finite v != 0, determine integers c and q such that
            |v| = c 2^q    and
            Q_MIN <= q <= Q_MAX    and
                either    2^(P-1) <= c < 2^P                 (normal)
                or        0 < c < 2^(P-1)  and  q = Q_MIN    (subnormal)
         */
        long bits = Double.doubleToRawLongBits(v);
        long t = bits & T_MASK;
        int bq = (int) (bits >>> (P - 1)) & BQ_MASK;
        boolean negative = false;
        if (bq < BQ_MASK) {
            if (bits < 0) {
                negative = true;
            }
            if (bq != 0) {
                // normal value. Here mq = -q
                int mq = -Q_MIN + 1 - bq;
                long c = C_MIN | t;
                // The fast path discussed in section 8.2 of [1].
                if (0 < mq && mq < P) {
                    long f = c >> mq;
                    if (f << mq == c) {
                        return new Decimal(f, 0, negative).toString();
                    }
                }
                return toDecimalImpl(-mq, c, 0, negative).toString();
            }
            if (t != 0) {
                // subnormal value
                return t < C_TINY
                        ? toDecimalImpl(Q_MIN, 10 * t, -1, negative).toString()
                        : toDecimalImpl(Q_MIN, t, 0, negative).toString();
            }
            return "0";
        }
        if (t != 0) {
            return "NaN";
        }
        return bits > 0 ? "Infinity" : "-Infinity";
    }

    private static Decimal toDecimalImpl(int q, long c, int dk, boolean negative) {
        /*
        The skeleton corresponds to figure 4 of [1].
        The efficient computations are those summarized in figure 7.

        Here's a correspondence between Java names and names in [1],
        expressed as approximate LaTeX source code and informally.
        Other names are identical.
        cb:     \bar{c}     "c-bar"
        cbr:    \bar{c}_r   "c-bar-r"
        cbl:    \bar{c}_l   "c-bar-l"

        vb:     \bar{v}     "v-bar"
        vbr:    \bar{v}_r   "v-bar-r"
        vbl:    \bar{v}_l   "v-bar-l"

        rop:    r_o'        "r-o-prime"
         */
        int out = (int) c & 0x1;
        long cb = c << 2;
        long cbr = cb + 2;
        long cbl;
        int k;
        /*
        flog10pow2(e) = floor(log_10(2^e))
        flog10threeQuartersPow2(e) = floor(log_10(3/4 2^e))
        flog2pow10(e) = floor(log_2(10^e))
         */
        if (c != C_MIN || q == Q_MIN) {
            // regular spacing
            cbl = cb - 2;
            k = MathUtils.flog10pow2(q);
        } else {
            // irregular spacing
            cbl = cb - 1;
            k = MathUtils.flog10threeQuartersPow2(q);
        }
        int h = q + MathUtils.flog2pow10(-k) + 2;

        // g1 and g0 are as in section 9.9.3 of [1], so g = g1 2^63 + g0
        long g1 = MathUtils.g1(k);
        long g0 = MathUtils.g0(k);

        long vb = rop(g1, g0, cb << h);
        long vbl = rop(g1, g0, cbl << h);
        long vbr = rop(g1, g0, cbr << h);

        long s = vb >> 2;
        if (s >= 100) {
            /*
            For n = 17, m = 1 the table in section 10 of [1] shows
                s' = floor(s / 10) = floor(s 115_292_150_460_684_698 / 2^60)
                   = floor(s 115_292_150_460_684_698 2^4 / 2^64)

            sp10 = 10 s'
            tp10 = 10 t'
            upin    iff    u' = sp10 10^k in Rv
            wpin    iff    w' = tp10 10^k in Rv
            See section 9.4 of [1].
             */
            long sp10 = 10 * MathUtils.multiplyHigh(s, 115_292_150_460_684_698L << 4);
            long tp10 = sp10 + 10;
            boolean upin = vbl + out <= sp10 << 2;
            boolean wpin = (tp10 << 2) + out <= vbr;
            if (upin != wpin) {
                return new Decimal(upin ? sp10 : tp10, k, negative);
            }
        }

        /*
        10 <= s < 100    or    s >= 100  and  u', w' not in Rv
        uin    iff    u = s 10^k in Rv
        win    iff    w = t 10^k in Rv
        See section 9.4 of [1].
         */
        long t = s + 1;
        boolean uin = vbl + out <= s << 2;
        boolean win = (t << 2) + out <= vbr;
        if (uin != win) {
            // Exactly one of u or w lies in Rv.
            return new Decimal(uin ? s : t, k + dk, negative);
        }
        /*
        Both u and w lie in Rv: determine the one closest to v.
        See section 9.4 of [1].
         */
        long cmp = vb - ((s + t) << 1);
        return new Decimal(cmp < 0 || (cmp == 0 && (s & 0x1) == 0) ? s : t, k + dk, negative);
    }

    /*
    Computes rop(cp g 2^(-127)), where g = g1 2^63 + g0
    See section 9.10 and figure 5 of [1].
     */
    private static long rop(long g1, long g0, long cp) {
        long x1 = MathUtils.multiplyHigh(g0, cp);
        long y0 = g1 * cp;
        long y1 = MathUtils.multiplyHigh(g1, cp);
        long z = (y0 >>> 1) + x1;
        long vbp = y1 + (z >>> 63);
        return vbp | ((z & MASK_63) + MASK_63) >>> 63;
    }
}
