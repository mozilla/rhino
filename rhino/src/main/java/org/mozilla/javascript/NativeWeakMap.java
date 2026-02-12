/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.mozilla.javascript.ClassDescriptor.Builder.value;
import static org.mozilla.javascript.ClassDescriptor.Destination.CTOR;
import static org.mozilla.javascript.ClassDescriptor.Destination.PROTO;

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

    private static final ClassDescriptor DESCRIPTOR;

    static {
        DESCRIPTOR =
                new ClassDescriptor.Builder(
                                CLASS_NAME,
                                0,
                                ClassDescriptor.typeError(),
                                NativeWeakMap::jsConstructor)
                        .withMethod(PROTO, "set", 2, NativeWeakMap::js_set)
                        .withMethod(PROTO, "delete", 1, NativeWeakMap::js_delete)
                        .withMethod(PROTO, "get", 1, NativeWeakMap::js_get)
                        .withMethod(PROTO, "has", 1, NativeWeakMap::js_has)
                        .withProp(
                                PROTO,
                                SymbolKey.TO_STRING_TAG,
                                value(CLASS_NAME, DONTENUM | READONLY))
                        .withProp(CTOR, SymbolKey.SPECIES, ScriptRuntimeES6::symbolSpecies)
                        .build();
    }

    private boolean instanceOfWeakMap = false;

    private transient WeakHashMap<Object, Object> map = new WeakHashMap<>();

    private static final Object NULL_VALUE = new Object();

    static Object init(Context cx, VarScope scope, boolean sealed) {
        return DESCRIPTOR.buildConstructor(cx, scope, new NativeObject(), sealed);
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    private static Scriptable jsConstructor(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeWeakMap nm = new NativeWeakMap();
        nm.instanceOfWeakMap = true;
        if (args.length > 0) {
            NativeMap.loadFromIterable(cx, f.getDeclarationScope(), nm, NativeMap.key(args));
        }
        ScriptRuntime.setBuiltinProtoAndParent(nm, f, nt, s, TopLevel.Builtins.WeakMap);
        return nm;
    }

    private static Object js_delete(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return realThis(thisObj, "delete").js_delete(NativeMap.key(args));
    }

    private Object js_delete(Object key) {
        if (!isValidKey(key)) {
            return Boolean.FALSE;
        }
        return map.remove(key) != null;
    }

    private static Object js_get(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return realThis(thisObj, "get").js_get(NativeMap.key(args));
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

    private static Object js_has(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return realThis(thisObj, "has").js_has(NativeMap.key(args));
    }

    private Object js_has(Object key) {
        if (!isValidKey(key)) {
            return Boolean.FALSE;
        }
        return map.containsKey(key);
    }

    private static Object js_set(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return realThis(thisObj, "set")
                .js_set(NativeMap.key(args), args.length > 1 ? args[1] : Undefined.instance);
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
        map.put(key, value);
        return this;
    }

    private static boolean isValidKey(Object key) {
        return ScriptRuntime.isUnregisteredSymbol(key) || ScriptRuntime.isObject(key);
    }

    private static NativeWeakMap realThis(Object thisObj, String name) {
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
