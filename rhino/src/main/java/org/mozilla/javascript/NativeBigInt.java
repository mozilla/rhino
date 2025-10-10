/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.math.BigInteger;

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
                null,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        js_asIntOrUintN(true, args),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.defineConstructorMethod(
                scope,
                "asUintN",
                2,
                null,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        js_asIntOrUintN(false, args),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "toString",
                0,
                null,
                NativeBigInt::js_toString,
                DONTENUM,
                DONTENUM | READONLY);
        // Alias toLocaleString to toString
        constructor.definePrototypeMethod(
                scope,
                "toLocaleString",
                0,
                null,
                NativeBigInt::js_toString,
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "toSource",
                0,
                null,
                NativeBigInt::js_toSource,
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "valueOf",
                0,
                null,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        toSelf(thisObj).bigIntValue,
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeProperty(
                SymbolKey.TO_STRING_TAG, CLASS_NAME, DONTENUM | READONLY);
        if (sealed) {
            constructor.sealObject();
            ((ScriptableObject) constructor.getPrototypeProperty()).sealObject();
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

        BigInteger modulus = BigInteger.ONE.shiftLeft(bits); // 2^bits

        if (isSigned) {
            return asSignedN(bigInt, bits, modulus);
        } else {
            return asUnsignedN(bigInt, modulus);
        }
    }

    private static BigInteger asUnsignedN(BigInteger bigInt, BigInteger modulus) {
        // For unsigned: return bigInt modulo 2^bits, ensuring non-negative result
        BigInteger result = bigInt.remainder(modulus);

        // Ensure result is non-negative for unsigned representation
        if (result.signum() < 0) {
            result = result.add(modulus);
        }

        return result;
    }

    private static BigInteger asSignedN(BigInteger bigInt, int bits, BigInteger modulus) {
        // For signed: use two's complement representation
        BigInteger halfModulus = BigInteger.ONE.shiftLeft(bits - 1); // 2^(bits-1)
        BigInteger minValue = halfModulus.negate(); // -2^(bits-1)
        BigInteger maxValue = halfModulus.subtract(BigInteger.ONE); // 2^(bits-1) - 1

        // If the number already fits in the signed range, return as is
        if (bigInt.compareTo(minValue) >= 0 && bigInt.compareTo(maxValue) <= 0) {
            return bigInt;
        }

        // Compute unsigned result first
        BigInteger result = asUnsignedN(bigInt, modulus);

        // Convert to signed range if needed
        if (result.compareTo(halfModulus) >= 0) {
            result = result.subtract(modulus);
        }

        return result;
    }

    @Override
    public String toString() {
        return ScriptRuntime.bigIntToString(bigIntValue, 10);
    }
}
