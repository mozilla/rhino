/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.mozilla.javascript.ClassDescriptor.Builder.alias;
import static org.mozilla.javascript.ClassDescriptor.Builder.value;
import static org.mozilla.javascript.ClassDescriptor.Destination.CTOR;
import static org.mozilla.javascript.ClassDescriptor.Destination.PROTO;

import java.util.List;
import java.util.Map;

public class NativeMap extends ScriptableObject {
    private static final long serialVersionUID = 1171922614280016891L;
    private static final String CLASS_NAME = "Map";
    static final String ITERATOR_TAG = "Map Iterator";

    private static final ClassDescriptor DESCRIPTOR;
    private static final ClassDescriptor ITER_DESCRIPTOR =
            ES6Iterator.makeDescriptor(ITERATOR_TAG, ITERATOR_TAG);

    static {
        DESCRIPTOR =
                new ClassDescriptor.Builder(
                                CLASS_NAME,
                                0,
                                ClassDescriptor.typeError(),
                                NativeMap::js_constructor)
                        .withMethod(CTOR, "groupBy", 2, NativeMap::jsGroupBy)
                        .withMethod(PROTO, "set", 2, NativeMap::js_set)
                        .withMethod(PROTO, "delete", 1, NativeMap::js_delete)
                        .withMethod(PROTO, "get", 1, NativeMap::js_get)
                        .withMethod(PROTO, "has", 1, NativeMap::js_has)
                        .withMethod(PROTO, "clear", 0, NativeMap::js_clear)
                        .withMethod(PROTO, "keys", 0, NativeMap::js_keys)
                        .withMethod(PROTO, "values", 0, NativeMap::js_values)
                        .withMethod(PROTO, "forEach", 1, NativeMap::js_forEach)
                        .withMethod(PROTO, "entries", 0, NativeMap::js_entries)
                        .withProp(PROTO, SymbolKey.ITERATOR, alias("entries", DONTENUM))
                        .withProp(
                                PROTO,
                                "size",
                                (thisObj) -> realThis(thisObj, "size").js_getSize(),
                                null,
                                DONTENUM)
                        .withProp(
                                PROTO,
                                NativeSet.GETSIZE,
                                (thisObj) -> realThis(thisObj, "size").js_getSize(),
                                null,
                                DONTENUM | PERMANENT)
                        .withProp(
                                PROTO,
                                SymbolKey.TO_STRING_TAG,
                                value(CLASS_NAME, DONTENUM | READONLY))
                        .withProp(CTOR, SymbolKey.SPECIES, ScriptRuntimeES6::symbolSpecies)
                        .build();
    }

    private final Hashtable entries = new Hashtable();

    private boolean instanceOfMap = false;

    static Object init(Context cx, VarScope scope, boolean sealed) {
        ES6Iterator.initialize(
                ITER_DESCRIPTOR,
                cx,
                (TopLevel) scope,
                new NativeCollectionIterator(ITERATOR_TAG),
                sealed,
                ITERATOR_TAG);
        return DESCRIPTOR.buildConstructor(cx, scope, new NativeObject(), sealed);
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    private static Scriptable js_constructor(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeMap nm = new NativeMap();
        nm.instanceOfMap = true;
        if (args.length > 0) {
            loadFromIterable(cx, s, nm, key(args));
        }
        ScriptRuntime.setBuiltinProtoAndParent(nm, f, nt, s, TopLevel.Builtins.Map);
        return nm;
    }

    private static Object jsGroupBy(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        Object items = args.length < 1 ? Undefined.instance : args[0];
        Object callback = args.length < 2 ? Undefined.instance : args[1];

        Map<Object, List<Object>> groups =
                AbstractEcmaObjectOperations.groupBy(
                        cx,
                        s,
                        CLASS_NAME,
                        "groupBy",
                        items,
                        callback,
                        AbstractEcmaObjectOperations.KEY_COERCION.COLLECTION);

        NativeMap map = (NativeMap) cx.newObject(s, "Map");

        for (Map.Entry<Object, List<Object>> entry : groups.entrySet()) {
            Scriptable elements = cx.newArray(s, entry.getValue().toArray());
            map.entries.put(entry.getKey(), elements);
        }

        return map;
    }

    private static Object js_set(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return realThis(thisObj, "set")
                .js_set(key(args), args.length > 1 ? args[1] : Undefined.instance);
    }

    private Object js_set(Object k, Object v) {
        // Special handling of "negative zero" from the spec.
        Object key = k;
        if ((key instanceof Number) && ((Number) key).doubleValue() == ScriptRuntime.negativeZero) {
            key = ScriptRuntime.zeroObj;
        }
        entries.put(key, v);
        return this;
    }

    private static Object js_delete(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return realThis(thisObj, "delete").js_delete(key(args));
    }

    private Object js_delete(Object arg) {
        return entries.deleteEntry(arg);
    }

    private static Object js_get(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return realThis(thisObj, "get").js_get(key(args));
    }

    private Object js_get(Object arg) {
        final Hashtable.Entry entry = entries.getEntry(arg);
        if (entry == null) {
            return Undefined.instance;
        }
        return entry.value;
    }

    private static Object js_has(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return realThis(thisObj, "has").js_has(key(args));
    }

    private Object js_has(Object arg) {
        return entries.has(arg);
    }

    private Object js_getSize() {
        return entries.size();
    }

    private static Object js_keys(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return realThis(thisObj, "keys")
                .js_iterator(f.getDeclarationScope(), NativeCollectionIterator.Type.KEYS);
    }

    private static Object js_values(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return realThis(thisObj, "values")
                .js_iterator(f.getDeclarationScope(), NativeCollectionIterator.Type.VALUES);
    }

    private static Object js_entries(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return realThis(thisObj, "entries")
                .js_iterator(f.getDeclarationScope(), NativeCollectionIterator.Type.BOTH);
    }

    private Object js_iterator(VarScope scope, NativeCollectionIterator.Type type) {
        return new NativeCollectionIterator(scope, ITERATOR_TAG, type, entries.iterator());
    }

    private static Object js_clear(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return realThis(thisObj, "clear").js_clear();
    }

    private Object js_clear() {
        entries.clear();
        return Undefined.instance;
    }

    private static Object js_forEach(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return realThis(thisObj, "forEach")
                .js_forEach(
                        cx,
                        s,
                        args.length > 0 ? args[0] : Undefined.instance,
                        args.length > 1 ? args[1] : Undefined.instance);
    }

    private Object js_forEach(Context cx, VarScope scope, Object arg1, Object arg2) {
        if (!(arg1 instanceof Callable)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.isnt.function", arg1, ScriptRuntime.typeof(arg1));
        }
        final Callable f = (Callable) arg1;

        boolean isStrict = cx.isStrictMode();
        for (Hashtable.Entry entry : entries) {
            // Per spec must convert every time so that primitives are always regenerated...
            Scriptable thisObj = ScriptRuntime.toObjectOrNull(cx, arg2, scope);

            if (thisObj == null && !isStrict) {
                thisObj = ScriptableObject.getTopLevelScope(scope).getGlobalThis();
            }
            if (thisObj == null) {
                thisObj = Undefined.SCRIPTABLE_UNDEFINED;
            }

            final Hashtable.Entry e = entry;
            f.call(cx, scope, thisObj, new Object[] {e.value, e.key, this});
        }
        return Undefined.instance;
    }

    /**
     * If an "iterable" object was passed to the constructor, there are many many things to do...
     * Make this static because NativeWeakMap has the exact same requirement.
     */
    static void loadFromIterable(Context cx, VarScope scope, ScriptableObject map, Object arg1) {
        if ((arg1 == null) || Undefined.instance.equals(arg1)) {
            return;
        }

        // Call the "[Symbol.iterator]" property as a function.
        final Object ito = ScriptRuntime.callIterator(arg1, cx, scope);
        if (Undefined.instance.equals(ito)) {
            // Per spec, ignore if the iterator is undefined
            return;
        }

        // Find the "add" function of our own prototype, since it might have
        // been replaced. Since we're not fully constructed yet, create a dummy instance
        // so that we can get our own prototype.
        Scriptable proto = ScriptableObject.getClassPrototype(scope, map.getClassName());
        var setCall = ScriptRuntime.getPropAndThis(proto, "set", cx, scope);
        Callable set = setCall.getCallable();
        ScriptRuntime.loadFromIterable(
                cx,
                scope,
                arg1,
                (key, value) -> set.call(cx, scope, map, new Object[] {key, value}));
    }

    private static NativeMap realThis(Object thisObj, String name) {
        NativeMap nm = LambdaConstructor.convertThisObject(thisObj, NativeMap.class);
        if (!nm.instanceOfMap) {
            // Check for "Map internal data tag"
            throw ScriptRuntime.typeErrorById("msg.incompat.call", name);
        }
        return nm;
    }

    /**
     * Extracts the key from the first args entry if any and takes care of the Delegator. This is
     * used by {@code NativeSet}, {@code NativeWeakMap}, and {@code NativeWekSet} also.
     *
     * @param args the args
     * @return the first argument (de-delegated) or undefined if there is no element in args
     */
    static Object key(Object[] args) {
        if (args.length > 0) {
            Object key = args[0];
            if (key instanceof Delegator) {
                return ((Delegator) key).getDelegee();
            }
            return key;
        }
        return Undefined.instance;
    }
}
