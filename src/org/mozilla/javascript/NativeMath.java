/* -*- Mode: java; tab-width: 4; indent-tabs-mode: 1; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * This class implements the Math native object.
 * See ECMA 15.8.
 * @author Norris Boyd
 */

final class NativeMath extends IdScriptableObject
{
    private static final long serialVersionUID = -8838847185801131569L;

    private static final Object MATH_TAG = "Math";
    private static final double LOG2E = 1.4426950408889634;

    static void init(Scriptable scope, boolean sealed)
    {
        NativeMath obj = new NativeMath();
        obj.activatePrototypeMap(MAX_ID);
        obj.setPrototype(getObjectPrototype(scope));
        obj.setParentScope(scope);
        if (sealed) { obj.sealObject(); }
        ScriptableObject.defineProperty(scope, "Math", obj,
                                        ScriptableObject.DONTENUM);
    }

    private NativeMath()
    {
    }

    @Override
    public String getClassName() { return "Math"; }

    @Override
    protected void initPrototypeId(int id)
    {
        if (id <= LAST_METHOD_ID) {
            String name;
            int arity;
            switch (id) {
              case Id_toSource: arity = 0; name = "toSource"; break;
              case Id_abs:      arity = 1; name = "abs";      break;
              case Id_acos:     arity = 1; name = "acos";     break;
              case Id_acosh:    arity = 1; name = "acosh";    break;
              case Id_asin:     arity = 1; name = "asin";     break;
              case Id_asinh:    arity = 1; name = "asinh";    break;
              case Id_atan:     arity = 1; name = "atan";     break;
              case Id_atanh:    arity = 1; name = "atanh";    break;
              case Id_atan2:    arity = 2; name = "atan2";    break;
              case Id_cbrt:     arity = 1; name = "cbrt";     break;
              case Id_ceil:     arity = 1; name = "ceil";     break;
              case Id_clz32:    arity = 1; name = "clz32";    break;
              case Id_cos:      arity = 1; name = "cos";      break;
              case Id_cosh:     arity = 1; name = "cosh";     break;
              case Id_exp:      arity = 1; name = "exp";      break;
              case Id_expm1:    arity = 1; name = "expm1";    break;
              case Id_floor:    arity = 1; name = "floor";    break;
              case Id_fround:   arity = 1; name = "fround";   break;
              case Id_hypot:    arity = 2; name = "hypot";    break;
              case Id_imul:     arity = 2; name = "imul";     break;
              case Id_log:      arity = 1; name = "log";      break;
              case Id_log1p:    arity = 1; name = "log1p";    break;
              case Id_log10:    arity = 1; name = "log10";    break;
              case Id_log2:     arity = 1; name = "log2";     break;
              case Id_max:      arity = 2; name = "max";      break;
              case Id_min:      arity = 2; name = "min";      break;
              case Id_pow:      arity = 2; name = "pow";      break;
              case Id_random:   arity = 0; name = "random";   break;
              case Id_round:    arity = 1; name = "round";    break;
              case Id_sign:     arity = 1; name = "sign";     break;
              case Id_sin:      arity = 1; name = "sin";      break;
              case Id_sinh:     arity = 1; name = "sinh";     break;
              case Id_sqrt:     arity = 1; name = "sqrt";     break;
              case Id_tan:      arity = 1; name = "tan";      break;
              case Id_tanh:     arity = 1; name = "tanh";     break;
              case Id_trunc:    arity = 1; name = "trunc";    break;
              default: throw new IllegalStateException(String.valueOf(id));
            }
            initPrototypeMethod(MATH_TAG, id, name, arity);
        } else {
            String name;
            double x;
            switch (id) {
              case Id_E:       x = Math.E;             name = "E";       break;
              case Id_PI:      x = Math.PI;            name = "PI";      break;
              case Id_LN10:    x = 2.302585092994046;  name = "LN10";    break;
              case Id_LN2:     x = 0.6931471805599453; name = "LN2";     break;
              case Id_LOG2E:   x = LOG2E;              name = "LOG2E";   break;
              case Id_LOG10E:  x = 0.4342944819032518; name = "LOG10E";  break;
              case Id_SQRT1_2: x = 0.7071067811865476; name = "SQRT1_2"; break;
              case Id_SQRT2:   x = 1.4142135623730951; name = "SQRT2";   break;
              default: throw new IllegalStateException(String.valueOf(id));
            }
            initPrototypeValue(id, name, ScriptRuntime.wrapNumber(x),
                               DONTENUM | READONLY | PERMANENT);
        }
    }

    @SuppressWarnings("SelfAssignment")
    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Scriptable thisObj, Object[] args)
    {
        if (!f.hasTag(MATH_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        double x;
        int methodId = f.methodId();
        switch (methodId) {
            case Id_toSource:
                return "Math";

            case Id_abs:
                x = ScriptRuntime.toNumber(args, 0);
                // abs(-0.0) should be 0.0, but -0.0 < 0.0 == false
                x = (x == 0.0) ? 0.0 : (x < 0.0) ? -x : x;
                break;

            case Id_acos:
            case Id_asin:
                x = ScriptRuntime.toNumber(args, 0);
                if (!Double.isNaN(x) && -1.0 <= x && x <= 1.0) {
                    x = (methodId == Id_acos) ? Math.acos(x) : Math.asin(x);
                } else {
                    x = Double.NaN;
                }
                break;

            case Id_acosh:
                x = ScriptRuntime.toNumber(args, 0);
                if (!Double.isNaN(x)) {
                    return Math.log(x + Math.sqrt(x*x - 1.0));
                }
                return Double.NaN;

            case Id_asinh:
                x = ScriptRuntime.toNumber(args, 0);
                if (Double.isInfinite(x)) {
                    return x;
                }
                if (!Double.isNaN(x)) {
                    if (x == 0) {
                        if (1 / x > 0) {
                            return 0.0;
                        }
                        return -0.0;
                    }
                    return Math.log(x + Math.sqrt(x*x + 1.0));
                }
                return Double.NaN;

            case Id_atan:
                x = ScriptRuntime.toNumber(args, 0);
                x = Math.atan(x);
                break;

            case Id_atanh:
                x = ScriptRuntime.toNumber(args, 0);
                if (!Double.isNaN(x) && -1.0 <= x && x <= 1.0) {
                    if (x == 0) {
                        if (1 / x > 0) {
                            return 0.0;
                        }
                        return -0.0;
                    }
                    return 0.5 * Math.log((x + 1.0) / (x - 1.0));
                }
                return Double.NaN;

            case Id_atan2:
                x = ScriptRuntime.toNumber(args, 0);
                x = Math.atan2(x, ScriptRuntime.toNumber(args, 1));
                break;

            case Id_cbrt:
                x = ScriptRuntime.toNumber(args, 0);
                x = Math.cbrt(x);
                break;

            case Id_ceil:
                x = ScriptRuntime.toNumber(args, 0);
                x = Math.ceil(x);
                break;

            case Id_clz32:
                x = ScriptRuntime.toNumber(args, 0);
                if (x == 0
                        || Double.isNaN(x)
                        || Double.isInfinite(x)) {
                    return 32;
                }
                long n = ScriptRuntime.toUint32(x);
                if (n == 0) {
                    return 32;
                }
                return 31 - Math.floor(Math.log(n >>> 0) * LOG2E);

            case Id_cos:
                x = ScriptRuntime.toNumber(args, 0);
                x = Double.isInfinite(x) ? Double.NaN : Math.cos(x);
                break;

            case Id_cosh:
                x = ScriptRuntime.toNumber(args, 0);
                x = Math.cosh(x);
                break;

            case Id_hypot:
                x = js_hypot(args);
                break;

            case Id_exp:
                x = ScriptRuntime.toNumber(args, 0);
                x = (x == Double.POSITIVE_INFINITY) ? x
                    : (x == Double.NEGATIVE_INFINITY) ? 0.0
                    : Math.exp(x);
                break;

            case Id_expm1:
                x = ScriptRuntime.toNumber(args, 0);
                x = Math.expm1(x);
                break;

            case Id_floor:
                x = ScriptRuntime.toNumber(args, 0);
                x = Math.floor(x);
                break;

            case Id_fround:
                x = ScriptRuntime.toNumber(args, 0);
                // Rely on Java to truncate down to a "float" here"
                x = (float) x;
                break;

            case Id_imul:
                return js_imul(args);

            case Id_log:
                x = ScriptRuntime.toNumber(args, 0);
                // Java's log(<0) = -Infinity; we need NaN
                x = (x < 0) ? Double.NaN : Math.log(x);
                break;

            case Id_log1p:
                x = ScriptRuntime.toNumber(args, 0);
                x = Math.log1p(x);
                break;

            case Id_log10:
                x = ScriptRuntime.toNumber(args, 0);
                x = Math.log10(x);
                break;

            case Id_log2:
                x = ScriptRuntime.toNumber(args, 0);
                // Java's log(<0) = -Infinity; we need NaN
                x = (x < 0) ? Double.NaN : Math.log(x) * LOG2E;
                break;

            case Id_max:
            case Id_min:
                x = (methodId == Id_max)
                    ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
                for (int i = 0; i != args.length; ++i) {
                    double d = ScriptRuntime.toNumber(args[i]);
                    if (Double.isNaN(d)) {
                        x = d; // NaN
                        break;
                    }
                    if (methodId == Id_max) {
                        // if (x < d) x = d; does not work due to -0.0 >= +0.0
                        x = Math.max(x, d);
                    } else {
                        x = Math.min(x, d);
                    }
                }
                break;

            case Id_pow:
                x = ScriptRuntime.toNumber(args, 0);
                x = js_pow(x, ScriptRuntime.toNumber(args, 1));
                break;

            case Id_random:
                x = Math.random();
                break;

            case Id_round:
                x = ScriptRuntime.toNumber(args, 0);
                if (!Double.isNaN(x) && !Double.isInfinite(x))
                {
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
                break;

            case Id_sign:
                x = ScriptRuntime.toNumber(args, 0);
                if (!Double.isNaN(x)) {
                    if (x == 0) {
                        if (1 / x > 0) {
                            return 0.0;
                        }
                        return -0.0;
                    }
                    return Math.signum(x);
                }
                return Double.NaN;

            case Id_sin:
                x = ScriptRuntime.toNumber(args, 0);
                x = Double.isInfinite(x) ? Double.NaN : Math.sin(x);
                break;

            case Id_sinh:
                x = ScriptRuntime.toNumber(args, 0);
                x = Math.sinh(x);
                break;

            case Id_sqrt:
                x = ScriptRuntime.toNumber(args, 0);
                x = Math.sqrt(x);
                break;

            case Id_tan:
                x = ScriptRuntime.toNumber(args, 0);
                x = Math.tan(x);
                break;

            case Id_tanh:
                x = ScriptRuntime.toNumber(args, 0);
                x = Math.tanh(x);
                break;

            case Id_trunc:
                x = ScriptRuntime.toNumber(args, 0);
                x = js_trunc(x);
                break;

            default: throw new IllegalStateException(String.valueOf(methodId));
        }
        return ScriptRuntime.wrapNumber(x);
    }

    // See Ecma 15.8.2.13
    private static double js_pow(double x, double y) {
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
                long y_long = (long)y;
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
                    long y_long = (long)y;
                    if (y_long == y && (y_long & 0x1) != 0) {
                        // y is odd integer
                        result = (y > 0) ? Double.NEGATIVE_INFINITY : -0.0;
                    } else {
                        result = (y > 0) ? Double.POSITIVE_INFINITY : 0.0;
                    }
                }
            }
        }
        return result;
    }

    // Based on code from https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Math/hypot
    private static double js_hypot(Object[] args)
    {
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

    private static double js_trunc(double d)
    {
        return ((d < 0.0) ? Math.ceil(d) : Math.floor(d));
    }

    // From EcmaScript 6 section 20.2.2.19
    private static int js_imul(Object[] args)
    {
        if (args == null) {
            return 0;
        }

        int x = ScriptRuntime.toInt32(args, 0);
        int y = ScriptRuntime.toInt32(args, 1);
        return x * y;
    }

// #string_id_map#

    @Override
    protected int findPrototypeId(String s)
    {
        int id;
// #generated# Last update: 2018-07-02 19:08:32 MESZ
        L0: { id = 0; String X = null; int c;
            L: switch (s.length()) {
            case 1: if (s.charAt(0)=='E') {id=Id_E; break L0;} break L;
            case 2: if (s.charAt(0)=='P' && s.charAt(1)=='I') {id=Id_PI; break L0;} break L;
            case 3: switch (s.charAt(0)) {
                case 'L': if (s.charAt(2)=='2' && s.charAt(1)=='N') {id=Id_LN2; break L0;} break L;
                case 'a': if (s.charAt(2)=='s' && s.charAt(1)=='b') {id=Id_abs; break L0;} break L;
                case 'c': if (s.charAt(2)=='s' && s.charAt(1)=='o') {id=Id_cos; break L0;} break L;
                case 'e': if (s.charAt(2)=='p' && s.charAt(1)=='x') {id=Id_exp; break L0;} break L;
                case 'l': if (s.charAt(2)=='g' && s.charAt(1)=='o') {id=Id_log; break L0;} break L;
                case 'm': c=s.charAt(2);
                    if (c=='n') { if (s.charAt(1)=='i') {id=Id_min; break L0;} }
                    else if (c=='x') { if (s.charAt(1)=='a') {id=Id_max; break L0;} }
                    break L;
                case 'p': if (s.charAt(2)=='w' && s.charAt(1)=='o') {id=Id_pow; break L0;} break L;
                case 's': if (s.charAt(2)=='n' && s.charAt(1)=='i') {id=Id_sin; break L0;} break L;
                case 't': if (s.charAt(2)=='n' && s.charAt(1)=='a') {id=Id_tan; break L0;} break L;
                } break L;
            case 4: switch (s.charAt(1)) {
                case 'N': X="LN10";id=Id_LN10; break L;
                case 'a': X="tanh";id=Id_tanh; break L;
                case 'b': X="cbrt";id=Id_cbrt; break L;
                case 'c': X="acos";id=Id_acos; break L;
                case 'e': X="ceil";id=Id_ceil; break L;
                case 'i': c=s.charAt(3);
                    if (c=='h') { if (s.charAt(0)=='s' && s.charAt(2)=='n') {id=Id_sinh; break L0;} }
                    else if (c=='n') { if (s.charAt(0)=='s' && s.charAt(2)=='g') {id=Id_sign; break L0;} }
                    break L;
                case 'm': X="imul";id=Id_imul; break L;
                case 'o': c=s.charAt(0);
                    if (c=='c') { if (s.charAt(2)=='s' && s.charAt(3)=='h') {id=Id_cosh; break L0;} }
                    else if (c=='l') { if (s.charAt(2)=='g' && s.charAt(3)=='2') {id=Id_log2; break L0;} }
                    break L;
                case 'q': X="sqrt";id=Id_sqrt; break L;
                case 's': X="asin";id=Id_asin; break L;
                case 't': X="atan";id=Id_atan; break L;
                } break L;
            case 5: switch (s.charAt(0)) {
                case 'L': X="LOG2E";id=Id_LOG2E; break L;
                case 'S': X="SQRT2";id=Id_SQRT2; break L;
                case 'a': c=s.charAt(1);
                    if (c=='c') { X="acosh";id=Id_acosh; }
                    else if (c=='s') { X="asinh";id=Id_asinh; }
                    else if (c=='t') {
                        c=s.charAt(4);
                        if (c=='2') { if (s.charAt(2)=='a' && s.charAt(3)=='n') {id=Id_atan2; break L0;} }
                        else if (c=='h') { if (s.charAt(2)=='a' && s.charAt(3)=='n') {id=Id_atanh; break L0;} }
                    }
                    break L;
                case 'c': X="clz32";id=Id_clz32; break L;
                case 'e': X="expm1";id=Id_expm1; break L;
                case 'f': X="floor";id=Id_floor; break L;
                case 'h': X="hypot";id=Id_hypot; break L;
                case 'l': c=s.charAt(4);
                    if (c=='0') { X="log10";id=Id_log10; }
                    else if (c=='p') { X="log1p";id=Id_log1p; }
                    break L;
                case 'r': X="round";id=Id_round; break L;
                case 't': X="trunc";id=Id_trunc; break L;
                } break L;
            case 6: c=s.charAt(0);
                if (c=='L') { X="LOG10E";id=Id_LOG10E; }
                else if (c=='f') { X="fround";id=Id_fround; }
                else if (c=='r') { X="random";id=Id_random; }
                break L;
            case 7: X="SQRT1_2";id=Id_SQRT1_2; break L;
            case 8: X="toSource";id=Id_toSource; break L;
            }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
        return id;
    }

    private static final int
        Id_toSource     =  1,
        Id_abs          =  2,
        Id_acos         =  3,
        Id_asin         =  4,
        Id_atan         =  5,
        Id_atan2        =  6,
        Id_ceil         =  7,
        Id_cos          =  8,
        Id_exp          =  9,
        Id_floor        = 10,
        Id_log          = 11,
        Id_max          = 12,
        Id_min          = 13,
        Id_pow          = 14,
        Id_random       = 15,
        Id_round        = 16,
        Id_sin          = 17,
        Id_sqrt         = 18,
        Id_tan          = 19,
        Id_cbrt         = 20,
        Id_cosh         = 21,
        Id_expm1        = 22,
        Id_hypot        = 23,
        Id_log1p        = 24,
        Id_log10        = 25,
        Id_sinh         = 26,
        Id_tanh         = 27,
        Id_imul         = 28,
        Id_trunc        = 29,
        Id_acosh        = 30,
        Id_asinh        = 31,
        Id_atanh        = 32,
        Id_sign         = 33,
        Id_log2         = 34,
        Id_fround       = 35,
        Id_clz32        = 36,

        LAST_METHOD_ID  = Id_clz32;

/* Missing from ES6:
    clz32
    fround
    log2
 */

    private static final int
        Id_E            = LAST_METHOD_ID + 1,
        Id_PI           = LAST_METHOD_ID + 2,
        Id_LN10         = LAST_METHOD_ID + 3,
        Id_LN2          = LAST_METHOD_ID + 4,
        Id_LOG2E        = LAST_METHOD_ID + 5,
        Id_LOG10E       = LAST_METHOD_ID + 6,
        Id_SQRT1_2      = LAST_METHOD_ID + 7,
        Id_SQRT2        = LAST_METHOD_ID + 8,

        MAX_ID = LAST_METHOD_ID + 8;

// #/string_id_map#
}
