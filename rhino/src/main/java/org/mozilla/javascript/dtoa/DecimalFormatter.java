package org.mozilla.javascript.dtoa;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class DecimalFormatter {
    private static final double MAX_FIXED = 1E21;

    /**
     * The algorithm of Number.prototype.toExponential. If fractionDigits is < 0, then it indicates
     * the special case that the value was previously undefined, which calls for a different
     * precision for the calculation.
     */
    public static String toExponential(double v, int fractionDigits) {
        assert Double.isFinite(v);

        if (fractionDigits < 0) {
            // In this case, we are supposed to use the "shortest possible representation."
            // This is what our usual toString implementation does
            return DoubleFormatter.toDecimal(v).toString(Decimal.Mode.TO_EXPONENTIAL);
        }

        boolean negative = Math.signum(v) < 0;
        double val = v;
        if (negative) {
            val = Math.abs(v);
        }
        BigDecimal bd = new BigDecimal(val, MathContext.UNLIMITED);
        if (bd.precision() > fractionDigits + 1) {
            bd = bd.round(new MathContext(fractionDigits + 1, RoundingMode.HALF_UP));
        }

        int exponent;
        if (bd.scale() >= 0) {
            exponent = bd.precision() - bd.scale() - 1;
        } else {
            // digits000
            exponent = bd.precision() + -bd.scale() - 1;
        }

        return toExponentialString(bd, exponent, fractionDigits, negative);
    }

    /** The algorithm of Number.prototype.toFixed(fractionDigits). */
    public static String toFixed(double v, int fractionDigits) {
        assert Double.isFinite(v);
        assert fractionDigits >= 0;
        boolean negative = Math.signum(v) < 0;
        double val = v;
        if (negative) {
            val = Math.abs(v);
        }
        if (val >= MAX_FIXED) {
            return DoubleFormatter.toString(v);
        }
        var bd = new BigDecimal(val, MathContext.UNLIMITED);
        if (bd.scale() > fractionDigits) {
            bd = bd.setScale(fractionDigits, RoundingMode.HALF_UP);
        }
        return toFixedString(bd, fractionDigits, negative);
    }

    /** The algorithm of Number.prototype.toPrecision() */
    public static String toPrecision(double v, int precision) {
        assert Double.isFinite(v);
        assert precision >= 1;
        boolean negative = Math.signum(v) < 0;
        double val;
        if (negative) {
            val = -v;
        } else {
            val = v;
        }
        var bd = new BigDecimal(val, MathContext.UNLIMITED);
        if (bd.precision() > precision) {
            bd = bd.round(new MathContext(precision, RoundingMode.HALF_UP));
        }

        int scale = bd.scale();
        int numDigits = bd.precision();
        int exponent;
        int fractionDigits;
        if (scale >= 0) {
            if (scale >= numDigits) {
                // 0.digits000
                fractionDigits = precision;
            } else {
                // dig.its
                fractionDigits = precision - (numDigits - scale);
            }
            exponent = numDigits - scale - 1;
        } else {
            // digits000
            fractionDigits = 0;
            exponent = numDigits + -scale - 1;
        }

        if (exponent < -6 || exponent >= precision) {
            return toExponentialString(bd, exponent, precision - 1, negative);
        }
        return toFixedString(bd, fractionDigits, negative);
    }

    private static String toFixedString(BigDecimal d, int fractionDigits, boolean negative) {
        int scale = d.scale();
        // Turns out that, in UNLIMITED scale mode, BigDecimal will not
        // produce a negative scale.
        assert (scale >= 0);
        String digits = d.unscaledValue().toString();
        int numDigits = digits.length();
        if (scale == 0 && fractionDigits == 0) {
            if (negative) {
                return "-" + digits;
            }
            return digits;
        }

        // Room for digits, -, ., extra 0
        var b = new StringBuilder(numDigits * 2 + 3);
        if (negative) {
            b.append('-');
        }
        if (scale >= numDigits) {
            // 0.digits000
            b.append("0.");
            fillZeroes(b, scale - numDigits);
            b.append(digits);
        } else {
            // dig.its000
            b.append(digits.substring(0, numDigits - scale));
            b.append('.');
            b.append(digits.substring(numDigits - scale));
        }
        fillZeroes(b, fractionDigits - scale);
        return b.toString();
    }

    private static String toExponentialString(
            BigDecimal d, int exponent, int fractionDigits, boolean negative) {
        String digits = d.unscaledValue().toString();
        int numDigits = digits.length();
        // Room for digits, ., -, e+000
        StringBuilder b = new StringBuilder(numDigits + fractionDigits + 7);

        if (negative) {
            b.append('-');
        }
        b.append(digits.charAt(0));
        if (numDigits > 1 || fractionDigits >= 1) {
            b.append('.');
            b.append(digits.substring(1));
            fillZeroes(b, fractionDigits - (numDigits - 1));
        }
        b.append('e');
        if (exponent >= 0) {
            b.append('+');
        }
        b.append(exponent);
        return b.toString();
    }

    private static void fillZeroes(StringBuilder b, int count) {
        for (int i = 0; i < count; i++) {
            b.append('0');
        }
    }
}
