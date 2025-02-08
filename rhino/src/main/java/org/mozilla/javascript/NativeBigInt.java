/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.math.BigInteger;
import java.util.Arrays;

/** This class implements the BigInt native object. */
final class NativeBigInt extends ScriptableObject {
    private static final long serialVersionUID = 1335609231306775449L;

    private static final String CLASS_NAME = "BigInt";

    private final BigInteger bigIntValue;

    static Object init(Context cx, Scriptable scope, boolean sealed) {
        LambdaConstructor constructor =
                new LambdaConstructor(
                        scope,
                        CLASS_NAME,
                        1,
                        NativeBigInt::js_constructorFunc,
                        NativeBigInt::js_constructor);
        constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);
        constructor.defineConstructorMethod(
                scope,
                "asIntN",
                2,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        js_asIntOrUintN(true, args),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.defineConstructorMethod(
                scope,
                "asUintN",
                2,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        js_asIntOrUintN(false, args),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope, "toString", 0, NativeBigInt::js_toString, DONTENUM, DONTENUM | READONLY);
        // Alias toLocaleString to toString
        constructor.definePrototypeMethod(
                scope,
                "toLocaleString",
                0,
                NativeBigInt::js_toString,
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope, "toSource", 0, NativeBigInt::js_toSource, DONTENUM, DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "valueOf",
                0,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        toSelf(thisObj).bigIntValue,
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeProperty(
                SymbolKey.TO_STRING_TAG, CLASS_NAME, DONTENUM | READONLY);
        if (sealed) {
            constructor.sealObject();
        }
        return constructor;
    }

    NativeBigInt(BigInteger bigInt) {
        bigIntValue = bigInt;
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    private static NativeBigInt toSelf(Scriptable thisObj) {
        return LambdaConstructor.convertThisObject(thisObj, NativeBigInt.class);
    }

    private static Object js_constructorFunc(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return (args.length >= 1) ? ScriptRuntime.toBigInt(args[0]) : BigInteger.ZERO;
    }

    private static Scriptable js_constructor(Context cx, Scriptable scope, Object[] args) {
        throw ScriptRuntime.typeErrorById("msg.no.new", CLASS_NAME);
    }

    private static Object js_toString(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        int base =
                (args.length == 0 || args[0] == Undefined.instance)
                        ? 10
                        : ScriptRuntime.toInt32(args[0]);
        BigInteger value = toSelf(thisObj).bigIntValue;
        return ScriptRuntime.bigIntToString(value, base);
    }

    private static Object js_toSource(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return "(new BigInt(" + ScriptRuntime.toString(toSelf(thisObj).bigIntValue) + "))";
    }

    private static Object js_asIntOrUintN(boolean isSigned, Object[] args) {
        int bits = ScriptRuntime.toIndex(args.length < 1 ? Undefined.instance : args[0]);
        BigInteger bigInt = ScriptRuntime.toBigInt(args.length < 2 ? Undefined.instance : args[1]);

        if (bits == 0) {
            return BigInteger.ZERO;
        }

        byte[] bytes = bigInt.toByteArray();

        int newBytesLen = (bits / Byte.SIZE) + 1;
        if (newBytesLen > bytes.length) {
            return bigInt;
        }

        byte[] newBytes = Arrays.copyOfRange(bytes, bytes.length - newBytesLen, bytes.length);

        int mod = bits % Byte.SIZE;
        if (isSigned) {
            if (mod == 0) {
                newBytes[0] = newBytes[1] < 0 ? (byte) -1 : 0;
            } else if ((newBytes[0] & (1 << (mod - 1))) != 0) {
                newBytes[0] = (byte) (newBytes[0] | (-1 << mod));
            } else {
                newBytes[0] = (byte) (newBytes[0] & ((1 << mod) - 1));
            }
        } else {
            newBytes[0] = (byte) (newBytes[0] & ((1 << mod) - 1));
        }
        return new BigInteger(newBytes);
    }

    @Override
    public String toString() {
        return ScriptRuntime.bigIntToString(bigIntValue, 10);
    }
}
