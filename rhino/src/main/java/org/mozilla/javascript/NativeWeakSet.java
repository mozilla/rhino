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
 * This is an implementation of the ES6 WeakSet class. It is very similar to NativeWeakMap, with the
 * exception being that it doesn't store any values. Java will GC the key only when there is no
 * longer any reference to it other than the weak reference. That means that it is important that
 * the "value" that we put in the WeakHashMap here is not one that contains the key.
 */
public class NativeWeakSet extends ScriptableObject {
    private static final long serialVersionUID = 2065753364224029534L;

    private static final String CLASS_NAME = "WeakSet";

    private boolean instanceOfWeakSet = false;

    private transient WeakHashMap<Scriptable, Boolean> map = new WeakHashMap<>();

    static Object init(Context cx, Scriptable scope, boolean sealed) {
        LambdaConstructor constructor =
                new LambdaConstructor(
                        scope,
                        CLASS_NAME,
                        0,
                        LambdaConstructor.CONSTRUCTOR_NEW,
                        NativeWeakSet::jsConstructor);
        constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);

        constructor.definePrototypeMethod(
                scope,
                "add",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        realThis(thisObj, "add").js_add(NativeMap.key(args)),
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
        NativeWeakSet ns = new NativeWeakSet();
        ns.instanceOfWeakSet = true;
        if (args.length > 0) {
            NativeSet.loadFromIterable(cx, scope, ns, NativeMap.key(args));
        }
        return ns;
    }

    private Object js_add(Object key) {
        // As the spec says, only a true "Object" can be the key to a WeakSet.
        // Use the default object equality here. ScriptableObject does not override
        // equals or hashCode, which means that in effect we are only keying on object identity.
        // This is all correct according to the ECMAscript spec.
        if (!isValidValue(key)) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(key));
        }
        // Add a value to the map, but don't make it the key -- otherwise the WeakHashMap
        // will never GC anything.
        map.put((Scriptable) key, Boolean.TRUE);
        return this;
    }

    private Object js_delete(Object key) {
        if (!isValidValue(key)) {
            return Boolean.FALSE;
        }
        return map.remove(key) != null;
    }

    private Object js_has(Object key) {
        if (!isValidValue(key)) {
            return Boolean.FALSE;
        }
        return map.containsKey(key);
    }

    private static boolean isValidValue(Object v) {
        return ScriptRuntime.isUnregisteredSymbol(v) || ScriptRuntime.isObject(v);
    }

    private static NativeWeakSet realThis(Scriptable thisObj, String name) {
        NativeWeakSet ns = LambdaConstructor.convertThisObject(thisObj, NativeWeakSet.class);
        if (!ns.instanceOfWeakSet) {
            // Check for "Set internal data tag"
            throw ScriptRuntime.typeErrorById("msg.incompat.call", name);
        }
        return ns;
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        map = new WeakHashMap<>();
    }
}
