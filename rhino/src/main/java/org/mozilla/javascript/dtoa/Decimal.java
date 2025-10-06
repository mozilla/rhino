package org.mozilla.javascript.dtoa;

/**
 * This class formats a decimal number into a string representation and stores the result in a
 * buffer. It is highly optimized for the case of formatting a double and only works if the number
 * has a maximum of 17 digits of precision.
 *
 * <p>Based on code by Guilietti:
 * https://github.com/c4f7fcce9cb06515/Schubfach/blob/3c92d3c9b1fead540616c918cdfef432bca53dfa/todec/src/math/FloatToDecimal.java
 */
public class Decimal {
    // Used for left-to-right digit extraction.
    private static final int MASK_28 = (1 << 28) - 1;

    /*
    Room for the longer of the forms.
    Always 17 significant digits.
        -ddddd.dddddddddddd             H + 2 characters
        -0.000000ddddddddddddddddd      H + 9 characters
            (JS will used fixed format down to -6 exponent)
        -d.ddddddddddddddddE-eee        H + 7 characters
        -ddddddddddddddddd0000          H + 5 characters
            (JS will use fixed format up to 21 exponent)
    where there are H digits d

    That means we need 26 characters for the largest possible number.
    We will use 32 because powers of two are good.
    */
    public final int MAX_CHARS = 32;

    private final long digits;
    private final int exponent;
    private final boolean negative;
    private int length;
    private final char[] buf = new char[MAX_CHARS];

    Decimal(long d, int e, boolean n) {
        this.digits = d;
        this.exponent = e;
        this.negative = n;
    }

    /**
     * Format the number according to the formatting rules in EcmaScript chapter 6, as defined for
     * the "Number::toString" operation, for radix 10.
     */
    @Override
    public String toString() {
        length = 0;

        /*
        For details not discussed here see section 10 of [1].

        Determine len such that
            10^(len-1) <= f < 10^len
         */
        int len = MathUtils.flog10pow2(Long.SIZE - Long.numberOfLeadingZeros(digits));
        if (digits >= MathUtils.pow10(len)) {
            len += 1;
        }

        /*
        Let fp and ep be the original f and e, respectively.
        Transform f and e to ensure
            10^(H-1) <= f < 10^H
            fp 10^ep = f 10^(e-H) = 0.f 10^e
         */
        long f = digits * MathUtils.pow10(DoubleFormatter.H - len);
        int e = exponent + len;
        /*
        The toChars?() methods perform left-to-right digits extraction
        using ints, provided that the arguments are limited to 8 digits.
        Therefore, split the H = 17 digits of f into:
            h = the most significant digit of f
            m = the next 8 most significant digits of f
            l = the last 8, least significant digits of f

        For n = 17, m = 8 the table in section 10 of [1] shows
            floor(f / 10^8) = floor(193_428_131_138_340_668 f / 2^84) =
            floor(floor(193_428_131_138_340_668 f / 2^64) / 2^20)
        and for n = 9, m = 8
            floor(hm / 10^8) = floor(1_441_151_881 hm / 2^57)
         */

        /*
         * Implementation note: The calculations above adjusts "f" so that
         * the most significant bits are at the front, with trailing zeroes.
         * That lets the bit of code below separate the digits into three
         * parts -- the first digit, the next 8, and the 8 after that.
         * We can then stringify it much more efficiently using this here clever
         * algorithm by Giulietti. We have adapted his original code a bit
         * to handle JavaScript, which requires fixed-format numbers for a
         * wider range of values before switching to exponential format.
         */
        long hm = Math.multiplyHigh(f, 193_428_131_138_340_668L) >>> 20;
        int l = (int) (f - 100_000_000L * hm);
        int h = (int) ((hm * 1_441_151_881L) >>> 57);
        int m = (int) (hm - 100_000_000L * h);

        if (negative) {
            append('-');
        }

        if (0 < e && e <= 8) {
            return toFixed(h, m, l, e);
        }
        if (8 < e && e <= 16) {
            return toFixedBigger(h, m, l, e);
        }
        if (16 < e && e <= 21) {
            return toFixedBiggest(h, m, l, e);
        }
        if (-6 < e && e <= 0) {
            return toFixedSmall(h, m, l, e);
        }
        return toExponential(h, m, l, e);
    }

    private String toFixed(int h, int m, int l, int e) {
        assert e <= 8;
        /*
        0 < e <= 8: plain format without leading zeroes.
        Left-to-right digits extraction:
        algorithm 1 in [3], with b = 10, k = 8, n = 28.
         */
        appendDigit(h);
        int y = y(m);
        int t;
        int i = 1;
        for (; i < e; ++i) {
            t = 10 * y;
            appendDigit(t >>> 28);
            y = t & MASK_28;
        }
        append('.');
        for (; i <= 8; ++i) {
            t = 10 * y;
            appendDigit(t >>> 28);
            y = t & MASK_28;
        }
        lowDigits(l);
        return makeString();
    }

    private String toFixedBigger(int h, int m, int l, int e) {
        assert e > 8 && e <= 16;
        /*
        8 > e <= 16: plain format without leading zeroes.
        Left-to-right digits extraction:
        But the first 9 characters are before the decimal.
         */
        appendDigit(h);
        append8Digits(m);
        int y = y(l);
        int t;
        int i = 9;
        for (; i < e; ++i) {
            t = 10 * y;
            appendDigit(t >>> 28);
            y = t & MASK_28;
        }
        append('.');
        for (; i <= 16; ++i) {
            t = 10 * y;
            appendDigit(t >>> 28);
            y = t & MASK_28;
        }
        trimZeroes();
        return makeString();
    }

    private String toFixedBiggest(int h, int m, int l, int e) {
        assert e > 16;
        /*
        16 < e: plain format with trailing zeroes.
         */
        appendDigit(h);
        append8Digits(m);
        append8Digits(l);
        for (int i = 17; i < e; i++) {
            append('0');
        }
        return makeString();
    }

    private String toFixedSmall(int h, int m, int l, int e) {
        assert e <= 0;
        // -3 < e <= 0: plain format with leading zeroes.
        appendDigit(0);
        append('.');
        for (; e < 0; ++e) {
            appendDigit(0);
        }
        appendDigit(h);
        append8Digits(m);
        lowDigits(l);
        return makeString();
    }

    private String toExponential(int h, int m, int l, int e) {
        // -3 >= e | e > 7: computerized scientific notation
        appendDigit(h);
        append('.');
        append8Digits(m);
        lowDigits(l);
        exponent(e - 1);
        return makeString();
    }

    private int y(int a) {
        /*
        Algorithm 1 in [3] needs computation of
            floor((a + 1) 2^n / b^k) - 1
        with a < 10^8, b = 10, k = 8, n = 28.
        Noting that
            (a + 1) 2^n <= 10^8 2^28 < 10^17
        For n = 17, m = 8 the table in section 10 of [1] leads to:
         */
        return (int) (Math.multiplyHigh((long) (a + 1) << 28, 193_428_131_138_340_668L) >>> 20) - 1;
    }

    private void lowDigits(int l) {
        if (l != 0) {
            append8Digits(l);
        }
        trimZeroes();
    }

    private void append8Digits(int m) {
        /*
        Left-to-right digits extraction:
        algorithm 1 in [3], with b = 10, k = 8, n = 28.
         */
        int y = y(m);
        for (int i = 0; i < 8; ++i) {
            int t = 10 * y;
            appendDigit(t >>> 28);
            y = t & MASK_28;
        }
    }

    private void exponent(int e) {
        assert e >= -999 && e <= 999;
        append('e');
        if (e < 0) {
            append('-');
            e = -e;
        } else {
            append('+');
        }
        if (e < 10) {
            appendDigit(e);
            return;
        }
        int d;
        if (e >= 100) {
            /*
            For n = 3, m = 2 the table in section 10 of [1] shows
                floor(e / 100) = floor(1_311 e / 2^17)
             */
            d = (e * 1_311) >>> 17;
            appendDigit(d);
            e -= 100 * d;
        }
        /*
        For n = 2, m = 1 the table in section 10 of [1] shows
            floor(e / 10) = floor(103 e / 2^10)
         */
        d = (e * 103) >>> 10;
        appendDigit(d);
        appendDigit(e - 10 * d);
    }

    private void trimZeroes() {
        while (length > 0 && buf[length - 1] == '0') {
            length--;
        }
        if (length > 0 && buf[length - 1] == '.') {
            length--;
        }
    }

    private void append(char ch) {
        buf[length++] = ch;
    }

    private void appendDigit(int d) {
        buf[length++] = (char) ('0' + d);
    }

    private String makeString() {
        return new String(buf, 0, length);
    }
}
