/* -*- Mode: java; tab-width: 4; indent-tabs-mode: 1; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.mozilla.javascript.ClassDescriptor.Builder.value;
import static org.mozilla.javascript.ClassDescriptor.Destination.CTOR;

import java.io.Serial;

/**
 * This class implements the Math native object. See ECMA 15.8.
 *
 * @author Norris Boyd
 */
final class NativeMath extends ScriptableObject {
    @Serial private static final long serialVersionUID = -8838847185801131569L;

    private static final String MATH_TAG = "Math";

    private static final double LN2 = 6.93147180559945286227e-01;
    private static final double LN10 = 2.302585092994046;
    private static final double LOG2E = 1.4426950408889634;
    private static final double LOG10E = 0.4342944819032518;
    private static final double SQRT1_2 = 0.7071067811865476;
    private static final double SQRT2 = 1.4142135623730951;
    private static final Double Double32 = 32d;

    // helper for log2
    private static final double TWO54 = 1.80143985094819840000e+16;
    private static final double IVLN2HI = 1.44269504072144627571e+00;
    private static final double IVLN2LO = 1.67517131648865118353e-10;
    private static final double LG1 = 6.666666666666735130e-01;
    private static final double LG2 = 3.999999999940941908e-01;
    private static final double LG3 = 2.857142874366239149e-01;
    private static final double LG4 = 2.222219843214978396e-01;
    private static final double LG5 = 1.818357216161805012e-01;
    private static final double LG6 = 1.531383769920937332e-01;
    private static final double LG7 = 1.479819860511658591e-01;

    private static final ClassDescriptor DESCRIPTOR;

    static {
        DESCRIPTOR =
                new ClassDescriptor.Builder(MATH_TAG)
                        .withProp(CTOR, "toSource", value("Math"))
                        .withMethod(CTOR, "abs", 1, NativeMath::abs)
                        .withMethod(CTOR, "acos", 1, NativeMath::acos)
                        .withMethod(CTOR, "acosh", 1, NativeMath::acosh)
                        .withMethod(CTOR, "asin", 1, NativeMath::asin)
                        .withMethod(CTOR, "asinh", 1, NativeMath::asinh)
                        .withMethod(CTOR, "atan", 1, NativeMath::atan)
                        .withMethod(CTOR, "atanh", 1, NativeMath::atanh)
                        .withMethod(CTOR, "atan2", 2, NativeMath::atan2)
                        .withMethod(CTOR, "cbrt", 1, NativeMath::cbrt)
                        .withMethod(CTOR, "ceil", 1, NativeMath::ceil)
                        .withMethod(CTOR, "clz32", 1, NativeMath::clz32)
                        .withMethod(CTOR, "cos", 1, NativeMath::cos)
                        .withMethod(CTOR, "cosh", 1, NativeMath::cosh)
                        .withMethod(CTOR, "exp", 1, NativeMath::exp)
                        .withMethod(CTOR, "expm1", 1, NativeMath::expm1)
                        .withMethod(CTOR, "f16round", 1, NativeMath::f16round)
                        .withMethod(CTOR, "floor", 1, NativeMath::floor)
                        .withMethod(CTOR, "fround", 1, NativeMath::fround)
                        .withMethod(CTOR, "hypot", 2, NativeMath::hypot)
                        .withMethod(CTOR, "imul", 2, NativeMath::imul)
                        .withMethod(CTOR, "log", 1, NativeMath::log)
                        .withMethod(CTOR, "log1p", 1, NativeMath::log1p)
                        .withMethod(CTOR, "log10", 1, NativeMath::log10)
                        .withMethod(CTOR, "log2", 1, NativeMath::log2)
                        .withMethod(CTOR, "max", 2, NativeMath::max)
                        .withMethod(CTOR, "min", 2, NativeMath::min)
                        .withMethod(CTOR, "pow", 2, NativeMath::pow)
                        .withMethod(CTOR, "random", 0, NativeMath::random)
                        .withMethod(CTOR, "round", 1, NativeMath::round)
                        .withMethod(CTOR, "sign", 1, NativeMath::sign)
                        .withMethod(CTOR, "sin", 1, NativeMath::sin)
                        .withMethod(CTOR, "sinh", 1, NativeMath::sinh)
                        .withMethod(CTOR, "sqrt", 1, NativeMath::sqrt)
                        .withMethod(CTOR, "tan", 1, NativeMath::tan)
                        .withMethod(CTOR, "tanh", 1, NativeMath::tanh)
                        .withMethod(CTOR, "trunc", 1, NativeMath::trunc)
                        .withProp(CTOR, "E", value(Math.E))
                        .withProp(CTOR, "PI", value(Math.PI))
                        .withProp(CTOR, "LN10", value(LN10))
                        .withProp(CTOR, "LN2", value(LN2))
                        .withProp(CTOR, "LOG2E", value(LOG2E))
                        .withProp(CTOR, "LOG10E", value(LOG10E))
                        .withProp(CTOR, "SQRT1_2", value(SQRT1_2))
                        .withProp(CTOR, "SQRT2", value(SQRT2))
                        .withProp(CTOR, SymbolKey.TO_STRING_TAG, value("Math", DONTENUM | READONLY))
                        .build();
    }

    static Object init(Context cx, VarScope scope, boolean sealed) {
        return DESCRIPTOR.populateGlobal(cx, scope, new NativeMath(), sealed);
    }

    private NativeMath() {}

    @Override
    public String getClassName() {
        return "Math";
    }

    private static Object abs(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        // abs(-0.0) should be 0.0, but -0.0 < 0.0 == false
        x = (x == 0.0) ? 0.0 : (x < 0.0) ? -x : x;

        return ScriptRuntime.wrapNumber(x);
    }

    private static Object acos(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        if (!Double.isNaN(x) && -1.0 <= x && x <= 1.0) {
            x = Math.acos(x);
        } else {
            x = Double.NaN;
        }
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object acosh(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        // Port of fdlibm e_acosh.c
        if (Double.isNaN(x) || x < 1.0) {
            return ScriptRuntime.NaNobj;
        }
        if (x >= 0x1p28) {
            if (Double.isInfinite(x)) {
                return x;
            }
            // sqrt(x^2 - 1) == x in double precision; avoids x*x overflow
            return StrictMath.log(x) + LN2;
        }
        if (x == 1.0) {
            return ScriptRuntime.zeroObj;
        }
        if (x > 2.0) {
            return StrictMath.log(2.0 * x - 1.0 / (x + Math.sqrt(x * x - 1.0)));
        }
        // 1 < x <= 2: log1p form keeps the precision close to 1
        double t = x - 1.0;
        return StrictMath.log1p(t + Math.sqrt(2.0 * t + t * t));
    }

    private static Object asin(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        if (!Double.isNaN(x) && -1.0 <= x && x <= 1.0) {
            x = Math.asin(x);
        } else {
            x = Double.NaN;
        }
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object asinh(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        if (Double.isNaN(x)) {
            return ScriptRuntime.NaNobj;
        }
        if (Double.isInfinite(x)) {
            return x;
        }
        if (x == 0) {
            if (1 / x > 0) {
                return ScriptRuntime.zeroObj;
            }
            return ScriptRuntime.negativeZeroObj;
        }
        // Port of fdlibm s_asinh.c; compute on |x| and restore the sign (asinh is odd)
        double t = Math.abs(x);
        double w;
        if (t < 0x1p-28) {
            // asinh(x) = x - x^3/6 + ..., so x is the correctly rounded result
            return x;
        }
        if (t > 0x1p28) {
            // sqrt(x^2 + 1) == |x| in double precision; avoids x*x overflow
            w = StrictMath.log(t) + LN2;
        } else if (t > 2.0) {
            w = StrictMath.log(2.0 * t + 1.0 / (Math.sqrt(x * x + 1.0) + t));
        } else {
            // log1p form avoids losing small inputs against 1.0
            double xx = x * x;
            w = StrictMath.log1p(t + xx / (1.0 + Math.sqrt(1.0 + xx)));
        }
        return x > 0 ? w : -w;
    }

    private static Object atan(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Math.atan(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object atanh(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        if (Double.isNaN(x) || x < -1.0 || x > 1.0) {
            return ScriptRuntime.NaNobj;
        }
        if (x == 0) {
            if (1 / x > 0) {
                return ScriptRuntime.zeroObj;
            }
            return ScriptRuntime.negativeZeroObj;
        }
        if (x == 1.0) {
            return Double.POSITIVE_INFINITY;
        }
        if (x == -1.0) {
            return Double.NEGATIVE_INFINITY;
        }
        // Port of fdlibm e_atanh.c; compute on |x| and restore the sign (atanh is odd)
        double t = Math.abs(x);
        double w;
        if (t < 0x1p-28) {
            // atanh(x) = x + x^3/3 + ..., so x is the correctly rounded result
            return x;
        } else if (t < 0.5) {
            double tt = t + t;
            w = 0.5 * StrictMath.log1p(tt + tt * t / (1.0 - t));
        } else {
            w = 0.5 * StrictMath.log1p((t + t) / (1.0 - t));
        }
        return x > 0 ? w : -w;
    }

    private static Object atan2(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Math.atan2(x, ScriptRuntime.toNumber(args, 1));
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object cbrt(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Math.cbrt(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object ceil(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Math.ceil(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object clz32(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        if (x == 0 || Double.isNaN(x) || Double.isInfinite(x)) {
            return Double32;
        }
        long n = ScriptRuntime.toUint32(x);
        if (n == 0) {
            return Double32;
        }

        int place = 0;
        if ((n & 0xFFFF0000) != 0) {
            place += 16;
            n >>>= 16;
        }
        if ((n & 0xFF00) != 0) {
            place += 8;
            n >>>= 8;
        }
        if ((n & 0xF0) != 0) {
            place += 4;
            n >>>= 4;
        }
        if ((n & 0b1100) != 0) {
            place += 2;
            n >>>= 2;
        }
        if ((n & 0b10) != 0) {
            place += 1;
            n >>>= 1;
        }
        if ((n & 0b1) != 0) {
            place += 1;
        }

        return (double) (32 - place);
    }

    private static Object cos(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Double.isInfinite(x) ? Double.NaN : Math.cos(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object cosh(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Math.cosh(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object exp(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x =
                (x == Double.POSITIVE_INFINITY)
                        ? x
                        : (x == Double.NEGATIVE_INFINITY) ? 0.0 : Math.exp(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object expm1(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Math.expm1(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object floor(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Math.floor(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object f16round(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        // Handle missing arguments
        if (args.length == 0) {
            return ScriptRuntime.NaNobj;
        }
        double x = ScriptRuntime.toNumber(args[0]);

        // Handle special cases
        if (Double.isNaN(x)) return ScriptRuntime.NaNobj;
        if (x == 0.0) return ScriptRuntime.wrapNumber(x); // Preserve sign of zero
        if (Double.isInfinite(x)) return ScriptRuntime.wrapNumber(x);

        // Extract components from double precision
        long bits = Double.doubleToLongBits(x);
        int sign = (int) (bits >>> 63);
        int exponent = (int) ((bits >>> 52) & 0x7FF);
        long mantissa = bits & 0x000FFFFFFFFFFFFFL;

        // Adjust from double bias (1023) to float16 bias (15)
        exponent = exponent - 1023 + 15;

        // Handle overflow to infinity
        if (exponent >= 31) {
            return ScriptRuntime.wrapNumber(
                    (sign != 0) ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
        }

        // Handle underflow and subnormal values (a float16 exponent of 0 is subnormal too)
        if (exponent <= 0) {
            return handleSubnormalF16(sign, exponent, mantissa);
        }

        // Normal value: round mantissa from 52 to 10 bits
        return handleNormalF16(sign, exponent, mantissa);
    }

    private static Object handleSubnormalF16(int sign, int exponent, long mantissa) {
        // Values below 2^-24 underflow to zero
        if (exponent < -10) {
            return ScriptRuntime.wrapNumber((sign != 0) ? -0.0 : 0.0);
        }

        // Special case: exactly 2^-25 rounds to zero (ties-to-even)
        if (exponent == -10 && mantissa == 0) {
            return ScriptRuntime.wrapNumber((sign != 0) ? -0.0 : 0.0);
        }

        // Special case: slightly above 2^-25 rounds to 2^-24
        if (exponent == -10 && mantissa > 0) {
            double smallestSubnormal = 0x1.0p-24; // 2^-24
            return ScriptRuntime.wrapNumber((sign != 0) ? -smallestSubnormal : smallestSubnormal);
        }

        // Convert to subnormal representation
        int totalShift = 42 + (1 - exponent);
        mantissa = mantissa | (1L << 52); // Add implicit 1 bit

        // Extract rounding information before shift
        long roundBit = (mantissa >> (totalShift - 1)) & 1;
        long stickyBits = mantissa & ((1L << (totalShift - 1)) - 1);

        // Shift to get 10-bit mantissa
        mantissa >>>= totalShift;

        // Apply ties-to-even rounding
        if (roundBit == 1 && (stickyBits != 0 || (mantissa & 1) == 1)) {
            mantissa++;
        }

        // Reconstruct subnormal value
        if (mantissa == 0) {
            return ScriptRuntime.wrapNumber((sign != 0) ? -0.0 : 0.0);
        }

        // Check for overflow to normal range
        if (mantissa >= (1L << 10)) {
            // Smallest normal = 2^-14
            return ScriptRuntime.wrapNumber((sign != 0) ? -6.103515625e-5 : 6.103515625e-5);
        }

        // Subnormal value = 2^-14 * (mantissa / 1024)
        double value = Math.scalb(mantissa / 1024.0, -14);
        return ScriptRuntime.wrapNumber((sign != 0) ? -value : value);
    }

    private static Object handleNormalF16(int sign, int exponent, long mantissa) {
        // Add implicit 1 bit for normal values
        long fullMantissa = mantissa | (1L << 52);

        // Extract rounding information
        long roundBit = (fullMantissa >> 41) & 1;
        long stickyBits = fullMantissa & ((1L << 41) - 1);
        fullMantissa >>>= 42;

        // Extract 10-bit mantissa (remove implicit 1)
        mantissa = fullMantissa & 0x3FF;

        // Apply ties-to-even rounding
        if (roundBit == 1 && (stickyBits != 0 || (mantissa & 1) == 1)) {
            mantissa++;
        }

        // Handle mantissa overflow
        if (mantissa >= (1L << 10)) {
            mantissa = 0;
            exponent++;
            if (exponent >= 31) {
                return ScriptRuntime.wrapNumber(
                        (sign != 0) ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
            }
        }

        return reconstructNormalF16(sign, exponent, mantissa);
    }

    private static Object reconstructNormalF16(int sign, int exponent, long mantissa) {
        long resultBits =
                ((long) sign << 63) | (((long) (exponent + 1023 - 15)) << 52) | (mantissa << 42);
        return ScriptRuntime.wrapNumber(Double.longBitsToDouble(resultBits));
    }

    private static Object fround(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        // Rely on Java to truncate down to a "float" here"
        float fx = (float) x;
        return ScriptRuntime.wrapNumber(fx);
    }

    // Same algorithm as V8: scale every value by the largest magnitude so the
    // squares can neither overflow nor underflow, and sum them with Kahan summation.
    private static Object hypot(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        if (args == null || args.length == 0) {
            return ScriptRuntime.zeroObj;
        }

        // Every argument is converted first; any "Infinity" takes precedence over NaN.
        double[] abs = new double[args.length];
        double max = 0.0;
        boolean hasNaN = false;
        for (int i = 0; i < args.length; i++) {
            double d = Math.abs(ScriptRuntime.toNumber(args[i]));
            abs[i] = d;
            if (Double.isNaN(d)) {
                hasNaN = true;
            } else if (d > max) {
                max = d;
            }
        }

        if (max == Double.POSITIVE_INFINITY) {
            return Double.POSITIVE_INFINITY;
        }
        if (hasNaN) {
            return ScriptRuntime.NaNobj;
        }
        if (max == 0.0) {
            return ScriptRuntime.zeroObj;
        }

        double sum = 0.0;
        double compensation = 0.0;
        for (double d : abs) {
            double n = d / max;
            double summand = n * n - compensation;
            double preliminary = sum + summand;
            compensation = (preliminary - sum) - summand;
            sum = preliminary;
        }
        return Math.sqrt(sum) * max;
    }

    private static Object imul(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        if (args == null) {
            return 0;
        }

        int x = ScriptRuntime.toInt32(args, 0);
        int y = ScriptRuntime.toInt32(args, 1);

        return ScriptRuntime.wrapNumber(x * y);
    }

    private static Object log(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        // Java's log(<0) = -Infinity; we need NaN
        x = (x < 0) ? Double.NaN : Math.log(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object log1p(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Math.log1p(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object log10(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Math.log10(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object log2(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        return ScriptRuntime.wrapNumber(log2(x));
    }

    /**
     * Port of FreeBSD msun e_log2.c (as used by V8). Unlike log(x) * LOG2E, it returns exact
     * results for powers of two.
     */
    private static double log2(double x) {
        long bits = Double.doubleToRawLongBits(x);
        int hx = (int) (bits >> 32);
        int lx = (int) bits;
        int k = 0;
        if (hx < 0x00100000) { // x < 2^-1022
            if (((hx & 0x7fffffff) | lx) == 0) {
                return Double.NEGATIVE_INFINITY; // log2(+-0) = -Infinity
            }
            if (hx < 0) {
                return Double.NaN; // log2(negative) = NaN
            }
            k -= 54; // subnormal, scale up x
            x *= TWO54;
            hx = (int) (Double.doubleToRawLongBits(x) >> 32);
        }
        if (hx >= 0x7ff00000) {
            return x + x; // Infinity or NaN
        }
        if (hx == 0x3ff00000 && lx == 0) {
            return 0.0; // log2(1) = +0
        }
        k += (hx >> 20) - 1023;
        hx &= 0x000fffff;
        int i = (hx + 0x95f64) & 0x100000;
        // normalize x or x/2
        x =
                Double.longBitsToDouble(
                        ((long) (hx | (i ^ 0x3ff00000)) << 32)
                                | (Double.doubleToRawLongBits(x) & 0xffffffffL));
        k += (i >> 20);
        double y = k;
        double f = x - 1.0;
        double hfsq = 0.5 * f * f;
        double r = kLog1p(f);
        double hi =
                Double.longBitsToDouble(Double.doubleToRawLongBits(f - hfsq) & 0xffffffff00000000L);
        double lo = (f - hi) - hfsq + r;
        double valHi = hi * IVLN2HI;
        double valLo = (lo + hi) * IVLN2LO + lo * IVLN2HI;
        double w = y + valHi;
        valLo += (y - w) + valHi;
        valHi = w;
        return valLo + valHi;
    }

    /**
     * k_log1p from FreeBSD msun k_log.h: log(1 + f) - f + f * f / 2 for sqrt(2)/2 - 1 <= f <=
     * sqrt(2) - 1.
     */
    private static double kLog1p(double f) {
        double s = f / (2.0 + f);
        double z = s * s;
        double w = z * z;
        double t1 = w * (LG2 + w * (LG4 + w * LG6));
        double t2 = z * (LG1 + w * (LG3 + w * (LG5 + w * LG7)));
        double hfsq = 0.5 * f * f;
        return s * (hfsq + t2 + t1);
    }

    private static Object max(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = Double.NEGATIVE_INFINITY;
        for (int i = 0; i != args.length; ++i) {
            double d = ScriptRuntime.toNumber(args[i]);
            // if (x < d) x = d; does not work due to -0.0 >= +0.0
            x = Math.max(x, d);
        }
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object min(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = Double.POSITIVE_INFINITY;
        for (int i = 0; i != args.length; ++i) {
            double d = ScriptRuntime.toNumber(args[i]);
            // if (x < d) x = d; does not work due to -0.0 >= +0.0
            x = Math.min(x, d);
        }
        return ScriptRuntime.wrapNumber(x);
    }

    // See Ecma 15.8.2.13
    private static Object pow(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        double y = ScriptRuntime.toNumber(args, 1);
        double result;
        if (Double.isNaN(y)) {
            // y is NaN, result is always NaN
            result = y;
        } else if (y == 0) {
            // Java's pow(NaN, 0) = NaN; we need 1
            result = 1.0;
        } else if (x == 0) {
            // Many differences from Java's Math.pow
            if (1 / x > 0) {
                result = (y > 0) ? 0 : Double.POSITIVE_INFINITY;
            } else {
                // x is -0, need to check if y is an odd integer
                long y_long = (long) y;
                if (y_long == y && (y_long & 0x1) != 0) {
                    result = (y > 0) ? -0.0 : Double.NEGATIVE_INFINITY;
                } else {
                    result = (y > 0) ? 0.0 : Double.POSITIVE_INFINITY;
                }
            }
        } else {
            result = Math.pow(x, y);
            if (Double.isNaN(result)) {
                // Check for broken Java implementations that gives NaN
                // when they should return something else
                if (y == Double.POSITIVE_INFINITY) {
                    if (x < -1.0 || 1.0 < x) {
                        result = Double.POSITIVE_INFINITY;
                    } else if (-1.0 < x && x < 1.0) {
                        result = 0;
                    }
                } else if (y == Double.NEGATIVE_INFINITY) {
                    if (x < -1.0 || 1.0 < x) {
                        result = 0;
                    } else if (-1.0 < x && x < 1.0) {
                        result = Double.POSITIVE_INFINITY;
                    }
                } else if (x == Double.POSITIVE_INFINITY) {
                    result = (y > 0) ? Double.POSITIVE_INFINITY : 0.0;
                } else if (x == Double.NEGATIVE_INFINITY) {
                    long y_long = (long) y;
                    if (y_long == y && (y_long & 0x1) != 0) {
                        // y is odd integer
                        result = (y > 0) ? Double.NEGATIVE_INFINITY : -0.0;
                    } else {
                        result = (y > 0) ? Double.POSITIVE_INFINITY : 0.0;
                    }
                }
            }
        }
        return ScriptRuntime.wrapNumber(result);
    }

    private static Object random(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return ScriptRuntime.wrapNumber(Math.random());
    }

    private static Object round(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        // Doubles with |x| >= 2^52 are integers already; Math.round would also
        // saturate at Long.MAX_VALUE for |x| >= 2^63
        if (!Double.isNaN(x) && !Double.isInfinite(x) && Math.abs(x) < 0x1p52) {
            // Round only finite x
            long l = Math.round(x);
            if (l == 0) {
                // We must propagate the sign of d into the result
                if (x < 0.0) {
                    x = ScriptRuntime.negativeZero;
                } else if (x != 0.0) {
                    x = 0.0;
                }
            } else {
                x = l;
            }
        }
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object sign(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        if (!Double.isNaN(x)) {
            if (x == 0) {
                if (1 / x > 0) {
                    return ScriptRuntime.zeroObj;
                }
                return ScriptRuntime.negativeZeroObj;
            }
            return Math.signum(x);
        }
        return ScriptRuntime.NaNobj;
    }

    private static Object sin(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Double.isInfinite(x) ? Double.NaN : Math.sin(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object sinh(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Math.sinh(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object sqrt(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Math.sqrt(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object tan(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Math.tan(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object tanh(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Math.tanh(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object trunc(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = ((x < 0.0) ? Math.ceil(x) : Math.floor(x));
        return ScriptRuntime.wrapNumber(x);
    }
}
