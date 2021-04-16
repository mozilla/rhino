/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.math.BigInteger;

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
        // TODO
        // addIdFunctionProperty(ctor, BIG_INT_TAG, ConstructorId_asIntN, "asIntN", 1);
        // addIdFunctionProperty(ctor, BIG_INT_TAG, ConstructorId_asUintN, "asUintN", 1);

        super.fillConstructorProperties(ctor);
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
            BigInteger val = (args.length >= 1) ? ScriptRuntime.toBigInt(args[0]) : BigInteger.ZERO;
            if (thisObj == null) {
                // new BigInt(val) creates a new BigInt object.
                return new NativeBigInt(val);
            }
            // TODO
            // // BigInt(val) converts val to a BigInt value.
            // return ScriptRuntime.wrapBigInt(val);
            return new NativeBigInt(val);

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
                    // TODO
                    // return ScriptRuntime.bigIntToString(value, base);
                    return "";
                }

            case Id_toSource:
                return "(new BigInt(" + ScriptRuntime.toString(value) + "))";

            case Id_valueOf:
                // TODO
                // return ScriptRuntime.wrapBigInt(value);
                return ScriptRuntime.wrapNumber(0.0);

            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
    }

    private static Object execConstructorCall(int id, Object[] args) {
        throw new IllegalArgumentException(String.valueOf(id));
    }

    @Override
    public String toString() {
        // TODO
        // return ScriptRuntime.bigIntToString(bigIntValue, 10);
        return "";
    }

    // #string_id_map#

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

    private static final int Id_constructor = 1,
            Id_toString = 2,
            Id_toLocaleString = 3,
            Id_toSource = 4,
            Id_valueOf = 5,
            MAX_PROTOTYPE_ID = 5;

    private BigInteger bigIntValue;
}
