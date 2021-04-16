/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.math.BigInteger;
import java.util.Arrays;

/** This class implements the BigInt native object. */
final class NativeBigInt extends IdScriptableObject {
    private static final long serialVersionUID = 1335609231306775449L;

    private static final Object BIG_INT_TAG = "BigInt";

    static void init(Scriptable scope, boolean sealed) {
        NativeBigInt obj = new NativeBigInt(BigInteger.ZERO);
        obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    NativeBigInt(BigInteger bigInt) {
        bigIntValue = bigInt;
    }

    @Override
    public String getClassName() {
        return "BigInt";
    }

    @Override
    protected void fillConstructorProperties(IdFunctionObject ctor) {
        addIdFunctionProperty(ctor, BIG_INT_TAG, ConstructorId_asIntN, "asIntN", 2);
        addIdFunctionProperty(ctor, BIG_INT_TAG, ConstructorId_asUintN, "asUintN", 2);

        super.fillConstructorProperties(ctor);
    }

    @Override
    protected void initPrototypeId(int id) {
        if (id == SymbolId_toStringTag) {
            initPrototypeValue(
                    SymbolId_toStringTag,
                    SymbolKey.TO_STRING_TAG,
                    getClassName(),
                    DONTENUM | READONLY);
            return;
        }

        String s;
        int arity;
        switch (id) {
            case Id_constructor:
                arity = 1;
                s = "constructor";
                break;
            case Id_toString:
                arity = 0;
                s = "toString";
                break;
            case Id_toLocaleString:
                arity = 0;
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
            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(BIG_INT_TAG, id, s, arity);
    }

    @Override
    public Object execIdCall(
            IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(BIG_INT_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        if (id == Id_constructor) {
            if (thisObj == null) {
                // new BigInt(val) throws TypeError.
                throw ScriptRuntime.typeErrorById("msg.not.ctor", BIG_INT_TAG);
            }
            BigInteger val = (args.length >= 1) ? ScriptRuntime.toBigInt(args[0]) : BigInteger.ZERO;
            // BigInt(val) converts val to a BigInteger value.
            return val;

        } else if (id < Id_constructor) {
            return execConstructorCall(id, args);
        }

        // The rest of BigInt.prototype methods require thisObj to be BigInt
        BigInteger value = ensureType(thisObj, NativeBigInt.class, f).bigIntValue;

        switch (id) {
            case Id_toString:
            case Id_toLocaleString:
                {
                    // toLocaleString is just an alias for toString for now
                    int base =
                            (args.length == 0 || args[0] == Undefined.instance)
                                    ? 10
                                    : ScriptRuntime.toInt32(args[0]);
                    return ScriptRuntime.bigIntToString(value, base);
                }

            case Id_toSource:
                return "(new BigInt(" + ScriptRuntime.toString(value) + "))";

            case Id_valueOf:
                return value;

            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
    }

    private static Object execConstructorCall(int id, Object[] args) {
        switch (id) {
            case ConstructorId_asIntN:
            case ConstructorId_asUintN:
                {
                    int bits =
                            ScriptRuntime.toIndex(args.length < 1 ? Undefined.instance : args[0]);
                    BigInteger bigInt =
                            ScriptRuntime.toBigInt(args.length < 2 ? Undefined.instance : args[1]);

                    if (bits == 0) {
                        return BigInteger.ZERO;
                    }

                    byte[] bytes = bigInt.toByteArray();

                    int newBytesLen = (bits / Byte.SIZE) + 1;
                    if (newBytesLen > bytes.length) {
                        return bigInt;
                    }

                    byte[] newBytes =
                            Arrays.copyOfRange(bytes, bytes.length - newBytesLen, bytes.length);

                    int mod = bits % Byte.SIZE;
                    switch (id) {
                        case ConstructorId_asIntN:
                            if (mod == 0) {
                                newBytes[0] = newBytes[1] < 0 ? (byte) -1 : 0;
                            } else if ((newBytes[0] & (1 << (mod - 1))) != 0) {
                                newBytes[0] |= -1 << mod;
                            } else {
                                newBytes[0] &= (1 << mod) - 1;
                            }
                            break;
                        case ConstructorId_asUintN:
                            newBytes[0] &= (1 << mod) - 1;
                            break;
                    }
                    return new BigInteger(newBytes);
                }

            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
    }

    @Override
    public String toString() {
        return ScriptRuntime.bigIntToString(bigIntValue, 10);
    }

    @Override
    protected int findPrototypeId(Symbol k) {
        if (SymbolKey.TO_STRING_TAG.equals(k)) {
            return SymbolId_toStringTag;
        }
        return 0;
    }

    @Override
    protected int findPrototypeId(String s) {
        int id;
        switch (s) {
            case "constructor":
                id = Id_constructor;
                break;
            case "toString":
                id = Id_toString;
                break;
            case "toLocaleString":
                id = Id_toLocaleString;
                break;
            case "toSource":
                id = Id_toSource;
                break;
            case "valueOf":
                id = Id_valueOf;
                break;
            default:
                id = 0;
                break;
        }
        return id;
    }

    private static final int ConstructorId_asIntN = -1,
            ConstructorId_asUintN = -2,
            Id_constructor = 1,
            Id_toString = 2,
            Id_toLocaleString = 3,
            Id_toSource = 4,
            Id_valueOf = 5,
            SymbolId_toStringTag = 6,
            MAX_PROTOTYPE_ID = SymbolId_toStringTag;

    private BigInteger bigIntValue;
}
