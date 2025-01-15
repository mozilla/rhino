/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * This class implements the Number native object.
 *
 * <p>See ECMA 15.7.
 *
 * @author Norris Boyd
 */
final class NativeNumber extends ScriptableObject {
    private static final long serialVersionUID = 3504516769741512101L;

    /**
     * @see https://www.ecma-international.org/ecma-262/6.0/#sec-number.max_safe_integer
     */
    public static final double MAX_SAFE_INTEGER = 9007199254740991.0; // Math.pow(2, 53) - 1

    private static final String CLASS_NAME = "Number";

    private static final int MAX_PRECISION = 100;
    private static final double MIN_SAFE_INTEGER = -MAX_SAFE_INTEGER;
    private static final double EPSILON = 2.220446049250313e-16;

    private final double doubleValue;// Math.pow(2, -52)

    static void init(Scriptable scope, boolean sealed) {
        LambdaConstructor constructor =
                new LambdaConstructor(
                        scope,
                        CLASS_NAME,
                        1,
                        NativeNumber::js_constructorFunc,
                        NativeNumber::js_constructor);
        constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);
        constructor.setPrototypeScriptable(new NativeNumber(0.0));

        final int propAttr =
                DONTENUM | PERMANENT | READONLY;

        constructor.defineProperty("NaN", ScriptRuntime.NaNobj, propAttr);
        constructor.defineProperty(
                "POSITIVE_INFINITY", ScriptRuntime.wrapNumber(Double.POSITIVE_INFINITY), propAttr);
        constructor.defineProperty(
                "NEGATIVE_INFINITY", ScriptRuntime.wrapNumber(Double.NEGATIVE_INFINITY), propAttr);
        constructor.defineProperty("MAX_VALUE", ScriptRuntime.wrapNumber(Double.MAX_VALUE), propAttr);
        constructor.defineProperty("MIN_VALUE", ScriptRuntime.wrapNumber(Double.MIN_VALUE), propAttr);
        constructor.defineProperty("MAX_SAFE_INTEGER", ScriptRuntime.wrapNumber(MAX_SAFE_INTEGER), propAttr);
        constructor.defineProperty("MIN_SAFE_INTEGER", ScriptRuntime.wrapNumber(MIN_SAFE_INTEGER), propAttr);
        constructor.defineProperty("EPSILON", ScriptRuntime.wrapNumber(EPSILON), propAttr);

        constructor.defineConstructorMethod(
                scope,
                "isFinite",
                1,
                NativeNumber::js_isFinite,
                DONTENUM,
                DONTENUM | READONLY
        );
        constructor.defineConstructorMethod(
                scope,
                "isNaN",
                1,
                NativeNumber::js_isNaN,
                DONTENUM,
                DONTENUM | READONLY
        );
        constructor.defineConstructorMethod(
                scope,
                "isInteger",
                1,
                NativeNumber::js_isInteger,
                DONTENUM,
                DONTENUM | READONLY
        );
        constructor.defineConstructorMethod(
                scope,
                "isSafeInteger",
                1,
                NativeNumber::js_isSafeInteger,
                DONTENUM,
                DONTENUM | READONLY
        );

        Object parseFloat = ScriptRuntime.getTopLevelProp(constructor, "parseFloat");
        if (parseFloat instanceof Function) {
            constructor.defineProperty(
                    "parseFloat",
                    parseFloat,
                    DONTENUM
            );
        }
        Object parseInt = ScriptRuntime.getTopLevelProp(constructor, "parseInt");
        if (parseInt instanceof Function) {
            constructor.defineProperty(
                    "parseInt",
                    parseInt,
                    DONTENUM
            );
        }

        ScriptableObject.defineProperty(scope, CLASS_NAME, constructor, DONTENUM);
        if (sealed) {
            constructor.sealObject();
        }
    }

    NativeNumber(double number) {
        doubleValue = number;
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    private static Scriptable js_constructor(Context cx, Scriptable scope, Object[] args) {
        double val = (args.length >= 1) ? ScriptRuntime.toNumeric(args[0]).doubleValue() : 0.0;
        return new NativeNumber(val);
    }

    private static Object js_constructorFunc(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return (args.length >= 1) ? ScriptRuntime.toNumeric(args[0]).doubleValue() : 0.0;
    }

    @Override
    protected void initPrototypeId(int id) {
        String s;
        int arity;
        switch (id) {
            case Id_constructor:
                arity = 1;
                s = "constructor";
                break;
            case Id_toString:
                arity = 1;
                s = "toString";
                break;
            case Id_toLocaleString:
                arity = 1;
                s = "toLocaleString";
                break;
            case Id_toSource:
                arity = 0;
                s = "toSource";
                break;
            case Id_valueOf:
                arity = 0;
                s = "valueOf";
                break;
            case Id_toFixed:
                arity = 1;
                s = "toFixed";
                break;
            case Id_toExponential:
                arity = 1;
                s = "toExponential";
                break;
            case Id_toPrecision:
                arity = 1;
                s = "toPrecision";
                break;
            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(NUMBER_TAG, id, s, arity);
    }

    @Override
    public Object execIdCall(
            IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(NUMBER_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        if (id == Id_constructor) {
            double val = (args.length >= 1) ? ScriptRuntime.toNumeric(args[0]).doubleValue() : 0.0;
            if (thisObj == null) {
                // new Number(val) creates a new Number object.
                return new NativeNumber(val);
            }
            // Number(val) converts val to a number value.
            return ScriptRuntime.wrapNumber(val);

        } else if (id < Id_constructor) {
            return execConstructorCall(id, args);
        }

        // The rest of Number.prototype methods require thisObj to be Number
        double value = ensureType(thisObj, NativeNumber.class, f).doubleValue;

        switch (id) {
            case Id_toString:
            case Id_toLocaleString:
                {
                    // toLocaleString is just an alias for toString for now
                    int base =
                            (args.length == 0 || Undefined.isUndefined(args[0]))
                                    ? 10
                                    : ScriptRuntime.toInt32(args[0]);
                    return ScriptRuntime.numberToString(value, base);
                }

            case Id_toSource:
                return "(new Number(" + ScriptRuntime.toString(value) + "))";

            case Id_valueOf:
                return ScriptRuntime.wrapNumber(value);

            case Id_toFixed:
                int precisionMin = cx.version < Context.VERSION_ES6 ? -20 : 0;
                return num_to(value, args, DToA.DTOSTR_FIXED, DToA.DTOSTR_FIXED, precisionMin, 0);

            case Id_toExponential:
                {
                    // Handle special values before range check
                    if (Double.isNaN(value)) {
                        return "NaN";
                    }
                    if (Double.isInfinite(value)) {
                        if (value >= 0) {
                            return "Infinity";
                        }
                        return "-Infinity";
                    }
                    // General case
                    return num_to(
                            value,
                            args,
                            DToA.DTOSTR_STANDARD_EXPONENTIAL,
                            DToA.DTOSTR_EXPONENTIAL,
                            0,
                            1);
                }

            case Id_toPrecision:
                {
                    // Undefined precision, fall back to ToString()
                    if (args.length == 0 || Undefined.isUndefined(args[0])) {
                        return ScriptRuntime.numberToString(value, 10);
                    }
                    // Handle special values before range check
                    if (Double.isNaN(value)) {
                        return "NaN";
                    }
                    if (Double.isInfinite(value)) {
                        if (value >= 0) {
                            return "Infinity";
                        }
                        return "-Infinity";
                    }
                    return num_to(value, args, DToA.DTOSTR_STANDARD, DToA.DTOSTR_PRECISION, 1, 0);
                }

            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
    }

    private static Number argToNumber(Object[] args) {
        if (args.length > 0) {
            if (args[0] instanceof Number) {
                return (Number) args[0];
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return ScriptRuntime.numberToString(doubleValue, 10);
    }

    private static String num_to(
            double val,
            Object[] args,
            int zeroArgMode,
            int oneArgMode,
            int precisionMin,
            int precisionOffset) {
        int precision;
        if (args.length == 0) {
            precision = 0;
            oneArgMode = zeroArgMode;
        } else {
            /* We allow a larger range of precision than
            ECMA requires; this is permitted by ECMA. */
            double p = ScriptRuntime.toInteger(args[0]);
            if (p < precisionMin || p > MAX_PRECISION) {
                String msg =
                        ScriptRuntime.getMessageById(
                                "msg.bad.precision", ScriptRuntime.toString(args[0]));
                throw ScriptRuntime.rangeError(msg);
            }
            precision = ScriptRuntime.toInt32(p);
        }
        StringBuilder sb = new StringBuilder();
        DToA.JS_dtostr(sb, oneArgMode, precision + precisionOffset, val);
        return sb.toString();
    }

    private static Object js_isFinite(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Number n = argToNumber(args);
        return n == null? Boolean.FALSE : isFinite(n);
    }

    static Object isFinite(Object val) {
        double nd = ScriptRuntime.toNumber(val);
        return ScriptRuntime.wrapBoolean(!Double.isInfinite(nd) && !Double.isNaN(nd));
    }

    private static Object js_isNaN(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Number n = argToNumber(args);
        return n == null ? Boolean.FALSE : isNaN(n);
    }

    private static Boolean isNaN(Number val) {
        if (val instanceof Double) {
            return((Double) val).isNaN();
        }

        double d = val.doubleValue();
        return Double.isNaN(d);
    }

    private static Object js_isInteger(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Number n = argToNumber(args);
        return n == null ? Boolean.FALSE : isInteger(n);
    }

    private static boolean isInteger(Number val) {
        if (val instanceof Double) {
            return isDoubleInteger((Double) val);
        }
        return isDoubleInteger(val.doubleValue());
    }

    private static boolean isDoubleInteger(Double d) {
        return !d.isInfinite() && !d.isNaN() && (Math.floor(d) == d);
    }

    private static boolean isDoubleInteger(double d) {
        return !Double.isInfinite(d) && !Double.isNaN(d) && (Math.floor(d) == d);
    }

    private static Object js_isSafeInteger(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Number n = argToNumber(args);
        return n == null ? Boolean.FALSE : isSafeInteger(n);
    }

    private static boolean isSafeInteger(Number val) {
        if (val instanceof Double) {
            return isDoubleSafeInteger((Double) val);
        }
        return isDoubleSafeInteger(val.doubleValue());
    }

    private static boolean isDoubleSafeInteger(Double d) {
        return isDoubleInteger(d)
                && (d <= MAX_SAFE_INTEGER)
                && (d >= MIN_SAFE_INTEGER);
    }

    private static boolean isDoubleSafeInteger(double d) {
        return isDoubleInteger(d) && (d <= MAX_SAFE_INTEGER) && (d >= MIN_SAFE_INTEGER);
    }
}
