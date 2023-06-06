/* -*- Mode: java; tab-width: 4; indent-tabs-mode: 1; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.ArrayList;
import java.util.List;

/**
 * This class implements the Reflect object.
 *
 * @author Ronald Brill
 */
final class NativeReflect extends IdScriptableObject {
    private static final long serialVersionUID = 2920773905356325445L;

    private static final Object REFLECT_TAG = "Reflect";

    static void init(Scriptable scope, boolean sealed) {
        NativeReflect obj = new NativeReflect();
        obj.activatePrototypeMap(LAST_METHOD_ID);
        obj.setPrototype(getObjectPrototype(scope));
        obj.setParentScope(scope);
        if (sealed) {
            obj.sealObject();
        }
        ScriptableObject.defineProperty(scope, "Reflect", obj, ScriptableObject.DONTENUM);
    }

    private NativeReflect() {}

    @Override
    public String getClassName() {
        return "Reflect";
    }

    @Override
    protected void initPrototypeId(int id) {
        if (id <= LAST_METHOD_ID) {
            String name;
            int arity;
            switch (id) {
                case Id_toSource:
                    arity = 0;
                    name = "toSource";
                    break;
                case Id_apply:
                    arity = 3;
                    name = "apply";
                    break;
                case Id_construct:
                    arity = 2;
                    name = "construct";
                    break;
                case Id_defineProperty:
                    arity = 3;
                    name = "defineProperty";
                    break;
                case Id_deleteProperty:
                    arity = 2;
                    name = "deleteProperty";
                    break;
                case Id_get:
                    arity = 2;
                    name = "get";
                    break;
                case Id_getOwnPropertyDescriptor:
                    arity = 2;
                    name = "getOwnPropertyDescriptor";
                    break;
                case Id_getPrototypeOf:
                    arity = 1;
                    name = "getPrototypeOf";
                    break;
                case Id_has:
                    arity = 2;
                    name = "has";
                    break;
                case Id_isExtensible:
                    arity = 1;
                    name = "isExtensible";
                    break;
                case Id_ownKeys:
                    arity = 1;
                    name = "ownKeys";
                    break;
                case Id_preventExtensions:
                    arity = 1;
                    name = "preventExtensions";
                    break;
                case Id_set:
                    arity = 3;
                    name = "set";
                    break;
                case Id_setPrototypeOf:
                    arity = 2;
                    name = "setPrototypeOf";
                    break;
                default:
                    throw new IllegalStateException(String.valueOf(id));
            }
            initPrototypeMethod(REFLECT_TAG, id, name, arity);
        }
    }

    @Override
    public Object execIdCall(
            IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(REFLECT_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }

        int methodId = f.methodId();
        switch (methodId) {
            case Id_toSource:
                return "Reflect";

            case Id_apply:
                return js_apply(cx, scope, args);
            case Id_construct:
                return js_construct(cx, scope, args);
            case Id_defineProperty:
                return js_defineProperty(cx, args);
            case Id_deleteProperty:
                return js_deleteProperty(args);
            case Id_get:
                return js_get(args);
            case Id_getOwnPropertyDescriptor:
                return js_getOwnPropertyDescriptor(cx, args);
            case Id_getPrototypeOf:
                return js_getPrototypeOf(args);
            case Id_has:
                return js_has(args);
            case Id_isExtensible:
                return js_isExtensible(args);
            case Id_ownKeys:
                return js_ownKeys(cx, scope, args);
            case Id_preventExtensions:
                return js_preventExtensions(args);
            case Id_set:
                return js_set(args);
            case Id_setPrototypeOf:
                return js_setPrototypeOf(args);

            default:
                throw new IllegalStateException(String.valueOf(methodId));
        }
    }

    private static Object js_apply(Context cx, Scriptable scope, Object[] args) {
        if (args.length < 3) {
            throw ScriptRuntime.typeErrorById(
                    "msg.method.missing.parameter",
                    "Reflect.apply",
                    "3",
                    Integer.toString(args.length));
        }

        Scriptable callable = ScriptableObject.ensureScriptable(args[0]);

        Scriptable thisObj = Undefined.SCRIPTABLE_UNDEFINED;
        if (args[1] instanceof Scriptable) {
            thisObj = (Scriptable) args[1];
        }

        if (ScriptRuntime.isSymbol(args[2])) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(args[2]));
        }
        ScriptableObject argumentsList = ScriptableObject.ensureScriptableObject(args[2]);

        return ScriptRuntime.applyOrCall(
                true, cx, scope, callable, new Object[] {thisObj, argumentsList});
    }

    private static Scriptable js_construct(Context cx, Scriptable scope, Object[] args) {
        if (args.length < 1) {
            throw ScriptRuntime.typeErrorById(
                    "msg.method.missing.parameter",
                    "Reflect.construct",
                    "3",
                    Integer.toString(args.length));
        }

        if (!(args[0] instanceof Function)) {
            throw ScriptRuntime.typeErrorById("msg.not.ctor", ScriptRuntime.typeof(args[0]));
        }

        Function ctor = (Function) args[0];
        if (args.length < 2) {
            return ctor.construct(cx, scope, ScriptRuntime.emptyArgs);
        }
        Object[] callArgs = ScriptRuntime.getApplyArguments(cx, args[1]);
        return ctor.construct(cx, scope, callArgs);
    }

    private static boolean js_defineProperty(Context cx, Object[] args) {
        if (args.length < 3) {
            throw ScriptRuntime.typeErrorById(
                    "msg.method.missing.parameter",
                    "Reflect.apply",
                    "3",
                    Integer.toString(args.length));
        }

        ScriptableObject obj = checkTarget(args);
        ScriptableObject desc = ScriptableObject.ensureScriptableObject(args[2]);

        try {
            obj.defineOwnProperty(cx, args[1], desc);
            return true;
        } catch (EcmaError e) {
            return false;
        }
    }

    private static boolean js_deleteProperty(Object[] args) {
        ScriptableObject obj = checkTarget(args);

        if (args.length > 1) {
            if (ScriptRuntime.isSymbol(args[1])) {
                return ScriptableObject.deleteProperty(obj, (Symbol) args[1]);
            }

            return ScriptableObject.deleteProperty(obj, ScriptRuntime.toString(args[1]));
        }
        return false;
    }

    private static Object js_get(Object[] args) {
        ScriptableObject obj = checkTarget(args);

        if (args.length > 1) {
            if (ScriptRuntime.isSymbol(args[1])) {
                Object prop = ScriptableObject.getProperty(obj, (Symbol) args[1]);
                return prop == Scriptable.NOT_FOUND ? Undefined.SCRIPTABLE_UNDEFINED : prop;
            }
            if (args[1] instanceof Double) {
                Object prop = ScriptableObject.getProperty(obj, ScriptRuntime.toIndex(args[1]));
                return prop == Scriptable.NOT_FOUND ? Undefined.SCRIPTABLE_UNDEFINED : prop;
            }

            Object prop = ScriptableObject.getProperty(obj, ScriptRuntime.toString(args[1]));
            return prop == Scriptable.NOT_FOUND ? Undefined.SCRIPTABLE_UNDEFINED : prop;
        }
        return Undefined.SCRIPTABLE_UNDEFINED;
    }

    private static Scriptable js_getOwnPropertyDescriptor(Context cx, Object[] args) {
        ScriptableObject obj = checkTarget(args);

        if (args.length > 1) {
            if (ScriptRuntime.isSymbol(args[1])) {
                ScriptableObject desc = obj.getOwnPropertyDescriptor(cx, args[1]);
                return desc == null ? Undefined.SCRIPTABLE_UNDEFINED : desc;
            }

            ScriptableObject desc =
                    obj.getOwnPropertyDescriptor(cx, ScriptRuntime.toString(args[1]));
            return desc == null ? Undefined.SCRIPTABLE_UNDEFINED : desc;
        }
        return Undefined.SCRIPTABLE_UNDEFINED;
    }

    private static Scriptable js_getPrototypeOf(Object[] args) {
        ScriptableObject obj = checkTarget(args);

        return obj.getPrototype();
    }

    private static boolean js_has(Object[] args) {
        ScriptableObject obj = checkTarget(args);

        if (args.length > 1) {
            if (ScriptRuntime.isSymbol(args[1])) {
                return ScriptableObject.hasProperty(obj, (Symbol) args[1]);
            }

            return ScriptableObject.hasProperty(obj, ScriptRuntime.toString(args[1]));
        }
        return false;
    }

    private static boolean js_isExtensible(Object[] args) {
        ScriptableObject obj = checkTarget(args);
        return obj.isExtensible();
    }

    private static Scriptable js_ownKeys(Context cx, Scriptable scope, Object[] args) {
        ScriptableObject obj = checkTarget(args);

        final List<Object> strings = new ArrayList<>();
        final List<Object> symbols = new ArrayList<>();

        for (Object o : obj.getIds(true, true)) {
            if (o instanceof Symbol) {
                symbols.add(o);
            } else {
                strings.add(ScriptRuntime.toString(o));
            }
        }

        Object[] keys = new Object[strings.size() + symbols.size()];
        System.arraycopy(strings.toArray(), 0, keys, 0, strings.size());
        System.arraycopy(symbols.toArray(), 0, keys, strings.size(), symbols.size());

        return cx.newArray(scope, keys);
    }

    private static boolean js_preventExtensions(Object[] args) {
        ScriptableObject obj = checkTarget(args);

        obj.preventExtensions();
        return true;
    }

    private static boolean js_set(Object[] args) {
        ScriptableObject obj = checkTarget(args);

        if (args.length > 1) {
            if (ScriptRuntime.isSymbol(args[1])) {
                obj.put((Symbol) args[1], obj, args[2]);
                return true;
            }
            if (args[1] instanceof Double) {
                obj.put(ScriptRuntime.toIndex(args[1]), obj, args[2]);
                return true;
            }

            obj.put(ScriptRuntime.toString(args[1]), obj, args[2]);
            return true;
        }
        return false;
    }

    private static boolean js_setPrototypeOf(Object[] args) {
        if (args.length < 2) {
            throw ScriptRuntime.typeErrorById(
                    "msg.method.missing.parameter",
                    "Reflect.js_setPrototypeOf",
                    "2",
                    Integer.toString(args.length));
        }

        ScriptableObject obj = checkTarget(args);

        if (obj.getPrototype() == args[1]) {
            return true;
        }

        if (!obj.isExtensible()) {
            return false;
        }

        if (args[1] == null) {
            obj.setPrototype(null);
            return true;
        }

        if (ScriptRuntime.isSymbol(args[1])) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(args[0]));
        }

        ScriptableObject proto = ScriptableObject.ensureScriptableObject(args[1]);
        if (obj.getPrototype() == proto) {
            return true;
        }

        // avoid cycles
        Scriptable p = proto;
        while (p != null) {
            if (obj == p) {
                return false;
            }
            p = p.getPrototype();
        }

        obj.setPrototype(proto);
        return true;
    }

    private static ScriptableObject checkTarget(Object[] args) {
        if (args.length == 0 || args[0] == null || args[0] == Undefined.instance) {
            Object argument = args.length == 0 ? Undefined.instance : args[0];
            throw ScriptRuntime.typeErrorById(
                    "msg.no.properties", ScriptRuntime.toString(argument));
        }

        if (ScriptRuntime.isSymbol(args[0])) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(args[0]));
        }
        return ScriptableObject.ensureScriptableObject(args[0]);
    }

    @Override
    protected int findPrototypeId(String s) {
        int id;
        switch (s) {
            case "toSource":
                id = Id_toSource;
                break;
            case "apply":
                id = Id_apply;
                break;
            case "construct":
                id = Id_construct;
                break;
            case "defineProperty":
                id = Id_defineProperty;
                break;
            case "deleteProperty":
                id = Id_deleteProperty;
                break;
            case "get":
                id = Id_get;
                break;
            case "getOwnPropertyDescriptor":
                id = Id_getOwnPropertyDescriptor;
                break;
            case "getPrototypeOf":
                id = Id_getPrototypeOf;
                break;
            case "has":
                id = Id_has;
                break;
            case "isExtensible":
                id = Id_isExtensible;
                break;
            case "ownKeys":
                id = Id_ownKeys;
                break;
            case "preventExtensions":
                id = Id_preventExtensions;
                break;
            case "set":
                id = Id_set;
                break;
            case "setPrototypeOf":
                id = Id_setPrototypeOf;
                break;

            default:
                id = 0;
                break;
        }
        return id;
    }

    private static final int Id_toSource = 1,
            Id_apply = 2,
            Id_construct = 3,
            Id_defineProperty = 4,
            Id_deleteProperty = 5,
            Id_get = 6,
            Id_getOwnPropertyDescriptor = 7,
            Id_getPrototypeOf = 8,
            Id_has = 9,
            Id_isExtensible = 10,
            Id_ownKeys = 11,
            Id_preventExtensions = 12,
            Id_set = 13,
            Id_setPrototypeOf = 14,
            LAST_METHOD_ID = Id_setPrototypeOf;
}
