/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * This class implements the Boolean native object. See ECMA 15.6.
 *
 * @author Norris Boyd
 */
final class NativeBoolean extends IdScriptableObject {
    private static final long serialVersionUID = -3716996899943880933L;

    private static final Object BOOLEAN_TAG = "Boolean";

    static void init(Scriptable scope, boolean sealed) {
        NativeBoolean obj = new NativeBoolean(false);
        obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    NativeBoolean(boolean b) {
        booleanValue = b;
    }

    @Override
    public String getClassName() {
        return "Boolean";
    }

    @Override
    public Object getDefaultValue(Class<?> typeHint) {
        // This is actually non-ECMA, but will be proposed
        // as a change in round 2.
        if (typeHint == ScriptRuntime.BooleanClass) return ScriptRuntime.wrapBoolean(booleanValue);
        return super.getDefaultValue(typeHint);
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
        initPrototypeMethod(BOOLEAN_TAG, id, s, arity);
    }

    @Override
    public Object execIdCall(
            IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(BOOLEAN_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();

        if (id == Id_constructor) {
            boolean b;
            if (args.length == 0) {
                b = false;
            } else {
                // see special handling in ScriptRuntime.toBoolean(Object)
                // avoidObjectDetection() is used to implement document.all
                // see Note on page
                //
                // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Boolean
                b =
                        args[0] instanceof ScriptableObject
                                        && ((ScriptableObject) args[0]).avoidObjectDetection()
                                ? false
                                : ScriptRuntime.toBoolean(args[0]);
            }
            if (thisObj == null) {
                // new Boolean(val) creates a new boolean object.
                return new NativeBoolean(b);
            }
            // Boolean(val) converts val to a boolean.
            return ScriptRuntime.wrapBoolean(b);
        }

        // The rest of Boolean.prototype methods require thisObj to be Boolean
        boolean value = ensureType(thisObj, NativeBoolean.class, f).booleanValue;

        switch (id) {
            case Id_toString:
                return value ? "true" : "false";

            case Id_toSource:
                return value ? "(new Boolean(true))" : "(new Boolean(false))";

            case Id_valueOf:
                return ScriptRuntime.wrapBoolean(value);
        }
        throw new IllegalArgumentException(String.valueOf(id));
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
            Id_toSource = 3,
            Id_valueOf = 4,
            MAX_PROTOTYPE_ID = 4;

    private boolean booleanValue;
}
