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
 * objects. Since there is no defined "equality" for objects, comparisons are done strictly by
 * object equality. Both ES6 and the java.util.WeakHashMap class have the same basic structure --
 * entries are removed automatically when the sole remaining reference to the key is a weak
 * reference. Therefore, we can use WeakHashMap as the basis of this implementation and preserve the
 * same semantics.
 */
public class NativeWeakMap extends ScriptableObject {
    private static final long serialVersionUID = 8670434366883930453L;

    private static final String CLASS_NAME = "WeakMap";

    private boolean instanceOfWeakMap = false;

    private transient WeakHashMap<Scriptable, Object> map = new WeakHashMap<>();

    private static final Object NULL_VALUE = new Object();

    static Object init(Context cx, Scriptable scope, boolean sealed) {
        LambdaConstructor constructor =
                new LambdaConstructor(
                        scope,
                        CLASS_NAME,
                        0,
                        LambdaConstructor.CONSTRUCTOR_NEW,
                        NativeWeakMap::jsConstructor);
        constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);

        constructor.definePrototypeMethod(
                scope,
                "set",
                2,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        realThis(thisObj, "set")
                                .js_set(
                                        NativeMap.key(args),
                                        args.length > 1 ? args[1] : Undefined.instance),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "delete",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        realThis(thisObj, "delete").js_delete(NativeMap.key(args)),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "get",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        realThis(thisObj, "get").js_get(NativeMap.key(args)),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "has",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        realThis(thisObj, "has").js_has(NativeMap.key(args)),
                DONTENUM,
                DONTENUM | READONLY);

        constructor.definePrototypeProperty(
                SymbolKey.TO_STRING_TAG, CLASS_NAME, DONTENUM | READONLY);

        ScriptRuntimeES6.addSymbolSpecies(cx, scope, constructor);
        if (sealed) {
            constructor.sealObject();
        }
        return constructor;
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    private static Scriptable jsConstructor(Context cx, Scriptable scope, Object[] args) {
        NativeWeakMap nm = new NativeWeakMap();
        nm.instanceOfWeakMap = true;
        if (args.length > 0) {
            NativeMap.loadFromIterable(cx, scope, nm, NativeMap.key(args));
        }
        return nm;
    }

    private Object js_delete(Object key) {
        if (!isValidKey(key)) {
            return Boolean.FALSE;
        }
        return map.remove(key) != null;
    }

    private Object js_get(Object key) {
        if (!isValidKey(key)) {
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
        if (!isValidKey(key)) {
            return Boolean.FALSE;
        }
        return map.containsKey(key);
    }

    private Object js_set(Object key, Object v) {
        // As the spec says, only a true "Object" can be the key to a WeakMap.
        // Use the default object equality here. ScriptableObject does not override
        // equals or hashCode, which means that in effect we are only keying on object identity.
        // This is all correct according to the ECMAscript spec.
        if (!isValidKey(key)) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(key));
        }
        // Map.get() does not distinguish between "not found" and a null value. So,
        // replace true null here with a marker so that we can re-convert in "get".
        final Object value = (v == null ? NULL_VALUE : v);
        map.put((Scriptable) key, value);
        return this;
    }

    private static boolean isValidKey(Object key) {
        return ScriptRuntime.isUnregisteredSymbol(key) || ScriptRuntime.isObject(key);
    }

    private static NativeWeakMap realThis(Scriptable thisObj, String name) {
        NativeWeakMap nm = LambdaConstructor.convertThisObject(thisObj, NativeWeakMap.class);
        if (!nm.instanceOfWeakMap) {
            // Check for "Map internal data tag"
            throw ScriptRuntime.typeErrorById("msg.incompat.call", name);
        }

        return nm;
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        map = new WeakHashMap<>();
    }
}
