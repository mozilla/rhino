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
 * This is an implementation of the ES6 WeakSet class. It is very similar to NativeWeakMap, with the
 * exception being that it doesn't store any values. Java will GC the key only when there is no
 * longer any reference to it other than the weak reference. That means that it is important that
 * the "value" that we put in the WeakHashMap here is not one that contains the key.
 */
public class NativeWeakSet extends ScriptableObject {
    private static final long serialVersionUID = 2065753364224029534L;

    private static final String CLASS_NAME = "WeakSet";

    private static final ClassDescriptor DESCRIPTOR;

    static {
        DESCRIPTOR =
                new ClassDescriptor.Builder(
                                CLASS_NAME,
                                0,
                                ClassDescriptor.typeError(),
                                NativeWeakSet::jsConstructor)
                        .withMethod(PROTO, "add", 1, NativeWeakSet::js_add)
                        .withMethod(PROTO, "delete", 1, NativeWeakSet::js_delete)
                        .withMethod(PROTO, "has", 1, NativeWeakSet::js_has)
                        .withProp(
                                PROTO,
                                SymbolKey.TO_STRING_TAG,
                                value(CLASS_NAME, DONTENUM | READONLY))
                        .withProp(CTOR, SymbolKey.SPECIES, ScriptRuntimeES6::symbolSpecies)
                        .build();
    }

    private boolean instanceOfWeakSet = false;

    private transient WeakHashMap<Object, Boolean> map = new WeakHashMap<>();

    static Object init(Context cx, VarScope scope, boolean sealed) {
        return DESCRIPTOR.buildConstructor(cx, scope, new NativeObject(), sealed);
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    private static Scriptable jsConstructor(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeWeakSet ns = new NativeWeakSet();
        ns.instanceOfWeakSet = true;
        if (args.length > 0) {
            NativeSet.loadFromIterable(cx, f.getDeclarationScope(), ns, NativeMap.key(args));
        }
        ns.setParentScope(f.getDeclarationScope());
        ns.setPrototype((Scriptable) f.getPrototypeProperty());
        return ns;
    }

    private static Object js_add(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeWeakSet realThis = realThis(thisObj, "add");
        var k = NativeMap.key(args);
        return realThis.js_add(k);
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
        map.put(key, Boolean.TRUE);
        return this;
    }

    private static Object js_delete(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeWeakSet realThis = realThis(thisObj, "add");
        var arg = NativeMap.key(args);
        return realThis.js_delete(arg);
    }

    private Object js_delete(Object key) {
        if (!isValidValue(key)) {
            return Boolean.FALSE;
        }
        return map.remove(key) != null;
    }

    private static Object js_has(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeWeakSet realThis = realThis(thisObj, "add");
        var arg = NativeMap.key(args);
        return realThis.js_has(arg);
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

    private static NativeWeakSet realThis(Object thisObj, String name) {
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
