/* -*- Mode: java; tab-width: 4; indent-tabs-mode: 1; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * This class implements the Math native object. See ECMA 15.8.
 *
 * @author Norris Boyd
 */
final class NativeMath extends ScriptableObject {
    private static final long serialVersionUID = -8838847185801131569L;

    private static final String MATH_TAG = "Math";
    private static final double LOG2E = 1.4426950408889634;
    private static final Double Double32 = Double.valueOf(32d);

    static Object init(Context cx, Scriptable scope, boolean sealed) {
        NativeMath math = new NativeMath();
        math.setPrototype(getObjectPrototype(scope));
        math.setParentScope(scope);

        math.defineProperty("toSource", "Math", DONTENUM | READONLY | PERMANENT);

        math.defineProperty(scope, "abs", 1, NativeMath::abs, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "acos", 1, NativeMath::acos, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "acosh", 1, NativeMath::acosh, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "asin", 1, NativeMath::asin, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "asinh", 1, NativeMath::asinh, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "atan", 1, NativeMath::atan, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "atanh", 1, NativeMath::atanh, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "atan2", 2, NativeMath::atan2, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "cbrt", 1, NativeMath::cbrt, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "ceil", 1, NativeMath::ceil, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "clz32", 1, NativeMath::clz32, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "cos", 1, NativeMath::cos, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "cosh", 1, NativeMath::cosh, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "exp", 1, NativeMath::exp, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "expm1", 1, NativeMath::expm1, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "floor", 1, NativeMath::floor, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "fround", 1, NativeMath::fround, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "hypot", 2, NativeMath::hypot, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "imul", 2, NativeMath::imul, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "log", 1, NativeMath::log, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "log1p", 1, NativeMath::log1p, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "log10", 1, NativeMath::log10, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "log2", 1, NativeMath::log2, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "max", 2, NativeMath::max, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "min", 2, NativeMath::min, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "pow", 2, NativeMath::pow, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "random", 0, NativeMath::random, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "round", 1, NativeMath::round, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "sign", 1, NativeMath::sign, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "sin", 1, NativeMath::sin, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "sinh", 1, NativeMath::sinh, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "sqrt", 1, NativeMath::sqrt, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "tan", 1, NativeMath::tan, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "tanh", 1, NativeMath::tanh, DONTENUM, DONTENUM | READONLY);
        math.defineProperty(scope, "trunc", 1, NativeMath::trunc, DONTENUM, DONTENUM | READONLY);

        math.defineProperty("E", Math.E, DONTENUM | READONLY | PERMANENT);
        math.defineProperty("PI", Math.PI, DONTENUM | READONLY | PERMANENT);
        math.defineProperty("LN10", 2.302585092994046, DONTENUM | READONLY | PERMANENT);
        math.defineProperty("LN2", 0.6931471805599453, DONTENUM | READONLY | PERMANENT);
        math.defineProperty("LOG2E", LOG2E, DONTENUM | READONLY | PERMANENT);
        math.defineProperty("LOG10E", 0.4342944819032518, DONTENUM | READONLY | PERMANENT);
        math.defineProperty("SQRT1_2", 0.7071067811865476, DONTENUM | READONLY | PERMANENT);
        math.defineProperty("SQRT2", 1.4142135623730951, DONTENUM | READONLY | PERMANENT);

        math.defineProperty(SymbolKey.TO_STRING_TAG, MATH_TAG, DONTENUM | READONLY);
        if (sealed) {
            math.sealObject();
        }
        return math;
    }

    private NativeMath() {}

    @Override
    public String getClassName() {
        return "Math";
    }

    private static Object abs(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        // abs(-0.0) should be 0.0, but -0.0 < 0.0 == false
        x = (x == 0.0) ? 0.0 : (x < 0.0) ? -x : x;

        return ScriptRuntime.wrapNumber(x);
    }

    private static Object acos(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        if (!Double.isNaN(x) && -1.0 <= x && x <= 1.0) {
            x = Math.acos(x);
        } else {
            x = Double.NaN;
        }
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object acosh(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        if (!Double.isNaN(x)) {
            return Double.valueOf(Math.log(x + Math.sqrt(x * x - 1.0)));
        }
        return ScriptRuntime.NaNobj;
    }

    private static Object asin(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        if (!Double.isNaN(x) && -1.0 <= x && x <= 1.0) {
            x = Math.asin(x);
        } else {
            x = Double.NaN;
        }
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object asinh(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        if (Double.isInfinite(x)) {
            return Double.valueOf(x);
        }
        if (!Double.isNaN(x)) {
            if (x == 0) {
                if (1 / x > 0) {
                    return ScriptRuntime.zeroObj;
                }
                return ScriptRuntime.negativeZeroObj;
            }
            return Double.valueOf(Math.log(x + Math.sqrt(x * x + 1.0)));
        }
        return ScriptRuntime.NaNobj;
    }

    private static Object atan(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Math.atan(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object atanh(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        if (!Double.isNaN(x) && -1.0 <= x && x <= 1.0) {
            if (x == 0) {
                if (1 / x > 0) {
                    return ScriptRuntime.zeroObj;
                }
                return ScriptRuntime.negativeZeroObj;
            }
            return Double.valueOf(0.5 * Math.log((1.0 + x) / (1.0 - x)));
        }
        return ScriptRuntime.NaNobj;
    }

    private static Object atan2(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Math.atan2(x, ScriptRuntime.toNumber(args, 1));
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object cbrt(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Math.cbrt(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object ceil(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Math.ceil(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object clz32(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
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

        return Double.valueOf(32 - place);
    }

    private static Object cos(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Double.isInfinite(x) ? Double.NaN : Math.cos(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object cosh(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Math.cosh(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object exp(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x =
                (x == Double.POSITIVE_INFINITY)
                        ? x
                        : (x == Double.NEGATIVE_INFINITY) ? 0.0 : Math.exp(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object expm1(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Math.expm1(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object floor(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Math.floor(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object fround(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        // Rely on Java to truncate down to a "float" here"
        float fx = (float) x;
        return ScriptRuntime.wrapNumber(fx);
    }

    // Based on code from
    // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Math/hypot
    private static Object hypot(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args == null) {
            return 0.0;
        }
        double y = 0.0;

        // Spec and tests say that any "Infinity" result takes precedence.
        boolean hasNaN = false;
        boolean hasInfinity = false;

        for (Object o : args) {
            double d = ScriptRuntime.toNumber(o);
            if (Double.isNaN(d)) {
                hasNaN = true;
            } else if (Double.isInfinite(d)) {
                hasInfinity = true;
            } else {
                y += d * d;
            }
        }

        if (hasInfinity) {
            return Double.POSITIVE_INFINITY;
        }
        if (hasNaN) {
            return Double.NaN;
        }
        return Math.sqrt(y);
    }

    private static Object imul(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args == null) {
            return 0;
        }

        int x = ScriptRuntime.toInt32(args, 0);
        int y = ScriptRuntime.toInt32(args, 1);

        return ScriptRuntime.wrapNumber(x * y);
    }

    private static Object log(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        // Java's log(<0) = -Infinity; we need NaN
        x = (x < 0) ? Double.NaN : Math.log(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object log1p(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Math.log1p(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object log10(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Math.log10(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object log2(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        // Java's log(<0) = -Infinity; we need NaN
        x = (x < 0) ? Double.NaN : Math.log(x) * LOG2E;
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object max(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = Double.NEGATIVE_INFINITY;
        for (int i = 0; i != args.length; ++i) {
            double d = ScriptRuntime.toNumber(args[i]);
            // if (x < d) x = d; does not work due to -0.0 >= +0.0
            x = Math.max(x, d);
        }
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object min(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = Double.POSITIVE_INFINITY;
        for (int i = 0; i != args.length; ++i) {
            double d = ScriptRuntime.toNumber(args[i]);
            // if (x < d) x = d; does not work due to -0.0 >= +0.0
            x = Math.min(x, d);
        }
        return ScriptRuntime.wrapNumber(x);
    }

    // See Ecma 15.8.2.13
    private static Object pow(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
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

    private static Object random(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return ScriptRuntime.wrapNumber(Math.random());
    }

    private static Object round(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        if (!Double.isNaN(x) && !Double.isInfinite(x)) {
            // Round only finite x
            long l = Math.round(x);
            if (l != 0) {
                x = l;
            } else {
                // We must propagate the sign of d into the result
                if (x < 0.0) {
                    x = ScriptRuntime.negativeZero;
                } else if (x != 0.0) {
                    x = 0.0;
                }
            }
        }
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object sign(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        if (!Double.isNaN(x)) {
            if (x == 0) {
                if (1 / x > 0) {
                    return ScriptRuntime.zeroObj;
                }
                return ScriptRuntime.negativeZeroObj;
            }
            return Double.valueOf(Math.signum(x));
        }
        return ScriptRuntime.NaNobj;
    }

    private static Object sin(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Double.isInfinite(x) ? Double.NaN : Math.sin(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object sinh(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Math.sinh(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object sqrt(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Math.sqrt(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object tan(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Math.tan(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object tanh(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = Math.tanh(x);
        return ScriptRuntime.wrapNumber(x);
    }

    private static Object trunc(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double x = ScriptRuntime.toNumber(args, 0);
        x = ((x < 0.0) ? Math.ceil(x) : Math.floor(x));
        return ScriptRuntime.wrapNumber(x);
    }
}
