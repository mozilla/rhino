/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import org.mozilla.javascript.dtoa.DecimalFormatter;

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
     * @see <a
     *     href="https://www.ecma-international.org/ecma-262/6.0/#sec-number.max_safe_integer">20.1.2.6
     *     Number.MAX_SAFE_INTEGER</a>
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
                scope,
                "isFinite",
                1,
                null,
                NativeNumber::js_isFinite,
                DONTENUM,
                DONTENUM | READONLY);
        constructor.defineConstructorMethod(
                scope, "isNaN", 1, null, NativeNumber::js_isNaN, DONTENUM, DONTENUM | READONLY);
        constructor.defineConstructorMethod(
                scope,
                "isInteger",
                1,
                null,
                NativeNumber::js_isInteger,
                DONTENUM,
                DONTENUM | READONLY);
        constructor.defineConstructorMethod(
                scope,
                "isSafeInteger",
                1,
                null,
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

        constructor.definePrototypeMethod(scope, "toString", 1, NativeNumber::js_toString);
        // Alias toLocaleString to toString
        constructor.definePrototypeMethod(scope, "toLocaleString", 0, NativeNumber::js_toString);
        constructor.definePrototypeMethod(scope, "toSource", 0, NativeNumber::js_toSource);
        constructor.definePrototypeMethod(scope, "valueOf", 0, NativeNumber::js_valueOf);
        constructor.definePrototypeMethod(scope, "toFixed", 1, NativeNumber::js_toFixed);
        constructor.definePrototypeMethod(
                scope, "toExponential", 1, NativeNumber::js_toExponential);
        constructor.definePrototypeMethod(scope, "toPrecision", 1, NativeNumber::js_toPrecision);

        ScriptableObject.defineProperty(scope, CLASS_NAME, constructor, DONTENUM);
        if (sealed) {
            constructor.sealObject();
            ((ScriptableObject) constructor.getPrototypeProperty()).sealObject();
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

    private static Object js_valueOf(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return toSelf(thisObj).doubleValue;
    }

    private static Object js_toFixed(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double value = toSelf(thisObj).doubleValue;

        int fractionDigits;
        if (args.length > 0 && !Undefined.isUndefined(args[0])) {
            double p = ScriptRuntime.toInteger(args[0]);
            int precisionMin = cx.version < Context.VERSION_ES6 ? -20 : 0;
            /* We allow a larger range of precision than
            ECMA requires; this is permitted by ECMA. */
            checkPrecision(p, precisionMin, args[0]);
            fractionDigits = ScriptRuntime.toInt32(p);
        } else {
            fractionDigits = 0;
        }

        if (!Double.isFinite(value)) {
            return ScriptRuntime.toString(value);
        }
        return DecimalFormatter.toFixed(value, fractionDigits);
    }

    private static Object js_toExponential(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double value = toSelf(thisObj).doubleValue;

        double p;
        boolean wasUndefined;
        if (args.length > 0 && !Undefined.isUndefined(args[0])) {
            wasUndefined = false;
            p = ScriptRuntime.toInteger(args[0]);
        } else {
            wasUndefined = true;
            p = 0.0;
        }

        if (!Double.isFinite(value)) {
            return ScriptRuntime.toString(value);
        }
        checkPrecision(p, 0.0, args.length > 0 ? args[0] : Undefined.instance);

        // Trigger the special handling for undefined, which requires that
        // we hold off on this bit until the checks above,.
        int fractionDigits = wasUndefined ? -1 : ScriptRuntime.toInt32(p);

        return DecimalFormatter.toExponential(value, fractionDigits);
    }

    private static Object js_toPrecision(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double value = toSelf(thisObj).doubleValue;
        // Undefined precision, fall back to ToString()
        if (args.length == 0 || Undefined.isUndefined(args[0])) {
            return ScriptRuntime.toString(value);
        }

        double p = ScriptRuntime.toInteger(args[0]);
        if (!Double.isFinite(value)) {
            return ScriptRuntime.toString(value);
        }
        checkPrecision(p, 1.0, args[0]);
        int precision = ScriptRuntime.toInt32(p);

        return DecimalFormatter.toPrecision(value, precision);
    }

    private static void checkPrecision(double p, double min, Object arg) {
        if (p < min || p > MAX_PRECISION) {
            String msg =
                    ScriptRuntime.getMessageById("msg.bad.precision", ScriptRuntime.toString(arg));
            throw ScriptRuntime.rangeError(msg);
        }
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

    private static Object js_isFinite(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Number n = argToNumber(args);
        return n == null ? Boolean.FALSE : isFinite(n);
    }

    static Object isFinite(Object val) {
        double nd = ScriptRuntime.toNumber(val);
        return Double.isFinite(nd);
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
        return Double.isFinite(d) && (Math.floor(d) == d);
    }

    private static boolean isDoubleSafeInteger(Double d) {
        return isDoubleInteger(d) && (d <= MAX_SAFE_INTEGER) && (d >= MIN_SAFE_INTEGER);
    }

    private static boolean isDoubleSafeInteger(double d) {
        return isDoubleInteger(d) && (d <= MAX_SAFE_INTEGER) && (d >= MIN_SAFE_INTEGER);
    }
}
