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

    private final double doubleValue;

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

        final int propAttr = DONTENUM | PERMANENT | READONLY;

        constructor.defineProperty("NaN", ScriptRuntime.NaNobj, propAttr);
        constructor.defineProperty(
                "POSITIVE_INFINITY", ScriptRuntime.wrapNumber(Double.POSITIVE_INFINITY), propAttr);
        constructor.defineProperty(
                "NEGATIVE_INFINITY", ScriptRuntime.wrapNumber(Double.NEGATIVE_INFINITY), propAttr);
        constructor.defineProperty(
                "MAX_VALUE", ScriptRuntime.wrapNumber(Double.MAX_VALUE), propAttr);
        constructor.defineProperty(
                "MIN_VALUE", ScriptRuntime.wrapNumber(Double.MIN_VALUE), propAttr);
        constructor.defineProperty(
                "MAX_SAFE_INTEGER", ScriptRuntime.wrapNumber(MAX_SAFE_INTEGER), propAttr);
        constructor.defineProperty(
                "MIN_SAFE_INTEGER", ScriptRuntime.wrapNumber(MIN_SAFE_INTEGER), propAttr);
        constructor.defineProperty("EPSILON", ScriptRuntime.wrapNumber(EPSILON), propAttr);

        constructor.defineConstructorMethod(
                scope, "isFinite", 1, NativeNumber::js_isFinite, DONTENUM, DONTENUM | READONLY);
        constructor.defineConstructorMethod(
                scope, "isNaN", 1, NativeNumber::js_isNaN, DONTENUM, DONTENUM | READONLY);
        constructor.defineConstructorMethod(
                scope, "isInteger", 1, NativeNumber::js_isInteger, DONTENUM, DONTENUM | READONLY);
        constructor.defineConstructorMethod(
                scope,
                "isSafeInteger",
                1,
                NativeNumber::js_isSafeInteger,
                DONTENUM,
                DONTENUM | READONLY);

        Object parseFloat = ScriptRuntime.getTopLevelProp(constructor, "parseFloat");
        if (parseFloat instanceof Function) {
            constructor.defineProperty("parseFloat", parseFloat, DONTENUM);
        }
        Object parseInt = ScriptRuntime.getTopLevelProp(constructor, "parseInt");
        if (parseInt instanceof Function) {
            constructor.defineProperty("parseInt", parseInt, DONTENUM);
        }

        constructor.definePrototypeMethod(
                scope, "toString", 1, NativeNumber::js_toString, DONTENUM, DONTENUM | READONLY);
        // Alias toLocaleString to toString
        constructor.definePrototypeMethod(
                scope,
                "toLocaleString",
                0,
                NativeNumber::js_toString,
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope, "toSource", 0, NativeNumber::js_toSource, DONTENUM, DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "valueOf",
                0,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        toSelf(thisObj).doubleValue,
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope, "toFixed", 1, NativeNumber::js_toFixed, DONTENUM, DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "toExponential",
                1,
                NativeNumber::js_toExponential,
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "toPrecision",
                1,
                NativeNumber::js_toPrecision,
                DONTENUM,
                DONTENUM | READONLY);

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
        double val = (args.length > 0) ? ScriptRuntime.toNumeric(args[0]).doubleValue() : 0.0;
        return new NativeNumber(val);
    }

    private static Object js_constructorFunc(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return (args.length > 0) ? ScriptRuntime.toNumeric(args[0]).doubleValue() : 0.0;
    }

    private static Object js_toFixed(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        int precisionMin = cx.version < Context.VERSION_ES6 ? -20 : 0;
        double value = toSelf(thisObj).doubleValue;
        return num_to(value, args, DToA.DTOSTR_FIXED, DToA.DTOSTR_FIXED, precisionMin, 0);
    }

    private static Object js_toExponential(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double value = toSelf(thisObj).doubleValue;
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
        return num_to(value, args, DToA.DTOSTR_STANDARD_EXPONENTIAL, DToA.DTOSTR_EXPONENTIAL, 0, 1);
    }

    private static Object js_toPrecision(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double value = toSelf(thisObj).doubleValue;
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

    private static NativeNumber toSelf(Scriptable thisObj) {
        return LambdaConstructor.convertThisObject(thisObj, NativeNumber.class);
    }

    private static Object js_toString(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        int base =
                (args.length == 0 || Undefined.isUndefined(args[0]))
                        ? 10
                        : ScriptRuntime.toInt32(args[0]);
        return ScriptRuntime.numberToString(toSelf(thisObj).doubleValue, base);
    }

    private static Object js_toSource(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return "(new Number(" + ScriptRuntime.toString(toSelf(thisObj).doubleValue) + "))";
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

    private static Object js_isFinite(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Number n = argToNumber(args);
        return n == null ? Boolean.FALSE : isFinite(n);
    }

    static Object isFinite(Object val) {
        double nd = ScriptRuntime.toNumber(val);
        return ScriptRuntime.wrapBoolean(!Double.isInfinite(nd) && !Double.isNaN(nd));
    }

    private static Object js_isNaN(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Number val = argToNumber(args);
        if (val == null) {
            return false;
        }
        if (val instanceof Double) {
            return ((Double) val).isNaN();
        }
        double d = val.doubleValue();
        return Double.isNaN(d);
    }

    private static Object js_isInteger(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Number val = argToNumber(args);
        if (val == null) {
            return false;
        }
        if (val instanceof Double) {
            return isDoubleInteger((Double) val);
        }
        return isDoubleInteger(val.doubleValue());
    }

    private static Object js_isSafeInteger(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Number val = argToNumber(args);
        if (val == null) {
            return false;
        }
        if (val instanceof Double) {
            return isDoubleSafeInteger((Double) val);
        }
        return isDoubleSafeInteger(val.doubleValue());
    }

    private static boolean isDoubleInteger(Double d) {
        return !d.isInfinite() && !d.isNaN() && (Math.floor(d) == d);
    }

    private static boolean isDoubleInteger(double d) {
        return !Double.isInfinite(d) && !Double.isNaN(d) && (Math.floor(d) == d);
    }

    private static boolean isDoubleSafeInteger(Double d) {
        return isDoubleInteger(d) && (d <= MAX_SAFE_INTEGER) && (d >= MIN_SAFE_INTEGER);
    }

    private static boolean isDoubleSafeInteger(double d) {
        return isDoubleInteger(d) && (d <= MAX_SAFE_INTEGER) && (d >= MIN_SAFE_INTEGER);
    }
}
