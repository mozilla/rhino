/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.WeakHashMap;

/**
 * This is an implementation of the ES6 WeakMap class. As per the spec, keys must be ordinary
 * objects. Since there is no defined "equality" for objects, comparisions are done strictly by
 * object equality. Both ES6 and the java.util.WeakHashMap class have the same basic structure --
 * entries are removed automatically when the sole remaining reference to the key is a weak
 * reference. Therefore, we can use WeakHashMap as the basis of this implementation and preserve the
 * same semantics.
 */
public class NativeWeakMap extends IdScriptableObject {
    private static final long serialVersionUID = 8670434366883930453L;

    private static final Object MAP_TAG = "WeakMap";

    private boolean instanceOfWeakMap = false;

    private transient WeakHashMap<Scriptable, Object> map = new WeakHashMap<>();

    private static final Object NULL_VALUE = new Object();

    static void init(Scriptable scope, boolean sealed) {
        NativeWeakMap m = new NativeWeakMap();
        m.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    @Override
    public String getClassName() {
        return "WeakMap";
    }

    @Override
    public Object execIdCall(
            IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {

        if (!f.hasTag(MAP_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        switch (id) {
            case Id_constructor:
                if (thisObj == null) {
                    NativeWeakMap nm = new NativeWeakMap();
                    nm.instanceOfWeakMap = true;
                    if (args.length > 0) {
                        NativeMap.loadFromIterable(cx, scope, nm, NativeMap.key(args));
                    }
                    return nm;
                }
                throw ScriptRuntime.typeErrorById("msg.no.new", "WeakMap");
            case Id_delete:
                return realThis(thisObj, f).js_delete(NativeMap.key(args));
            case Id_get:
                return realThis(thisObj, f).js_get(NativeMap.key(args));
            case Id_has:
                return realThis(thisObj, f).js_has(NativeMap.key(args));
            case Id_set:
                return realThis(thisObj, f)
                        .js_set(
                                NativeMap.key(args),
                                args.length > 1 ? args[1] : Undefined.instance);
        }
        throw new IllegalArgumentException(
                "WeakMap.prototype has no method: " + f.getFunctionName());
    }

    private Object js_delete(Object key) {
        if (!ScriptRuntime.isObject(key)) {
            return Boolean.FALSE;
        }
        return Boolean.valueOf(map.remove(key) != null);
    }

    private Object js_get(Object key) {
        if (!ScriptRuntime.isObject(key)) {
            return Undefined.instance;
        }
        Object result = map.get(key);
        if (result == null) {
            return Undefined.instance;
        } else if (result == NULL_VALUE) {
            return null;
        }
        return result;
    }

    private Object js_has(Object key) {
        if (!ScriptRuntime.isObject(key)) {
            return Boolean.FALSE;
        }
        return Boolean.valueOf(map.containsKey(key));
    }

    private Object js_set(Object key, Object v) {
        // As the spec says, only a true "Object" can be the key to a WeakMap.
        // Use the default object equality here. ScriptableObject does not override
        // equals or hashCode, which means that in effect we are only keying on object identity.
        // This is all correct according to the ECMAscript spec.
        if (!ScriptRuntime.isObject(key)) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(key));
        }
        // Map.get() does not distinguish between "not found" and a null value. So,
        // replace true null here with a marker so that we can re-convert in "get".
        final Object value = (v == null ? NULL_VALUE : v);
        map.put((Scriptable) key, value);
        return this;
    }

    private static NativeWeakMap realThis(Scriptable thisObj, IdFunctionObject f) {
        final NativeWeakMap nm = ensureType(thisObj, NativeWeakMap.class, f);
        if (!nm.instanceOfWeakMap) {
            // Check for "Map internal data tag"
            throw ScriptRuntime.typeErrorById("msg.incompat.call", f.getFunctionName());
        }

        return nm;
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

        String s, fnName = null;
        int arity;
        switch (id) {
            case Id_constructor:
                arity = 0;
                s = "constructor";
                break;
            case Id_delete:
                arity = 1;
                s = "delete";
                break;
            case Id_get:
                arity = 1;
                s = "get";
                break;
            case Id_has:
                arity = 1;
                s = "has";
                break;
            case Id_set:
                arity = 2;
                s = "set";
                break;
            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(MAP_TAG, id, s, fnName, arity);
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
            case "delete":
                id = Id_delete;
                break;
            case "get":
                id = Id_get;
                break;
            case "has":
                id = Id_has;
                break;
            case "set":
                id = Id_set;
                break;
            default:
                id = 0;
                break;
        }
        return id;
    }

    private static final int Id_constructor = 1,
            Id_delete = 2,
            Id_get = 3,
            Id_has = 4,
            Id_set = 5,
            SymbolId_toStringTag = 6,
            MAX_PROTOTYPE_ID = SymbolId_toStringTag;

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        map = new WeakHashMap<>();
    }
}
