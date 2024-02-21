/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

public class NativeMap extends IdScriptableObject {
    private static final long serialVersionUID = 1171922614280016891L;
    private static final Object MAP_TAG = "Map";
    static final String ITERATOR_TAG = "Map Iterator";

    private final Hashtable entries = new Hashtable();

    private boolean instanceOfMap = false;

    static void init(Context cx, Scriptable scope, boolean sealed) {
        NativeMap obj = new NativeMap();
        IdFunctionObject constructor = obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, false);

        ScriptableObject desc = (ScriptableObject) cx.newObject(scope);
        desc.put("enumerable", desc, Boolean.FALSE);
        desc.put("configurable", desc, Boolean.TRUE);
        desc.put("get", desc, obj.get(NativeSet.GETSIZE, obj));
        obj.defineOwnProperty(cx, "size", desc);

        ScriptRuntimeES6.addSymbolSpecies(cx, scope, constructor);

        if (sealed) {
            obj.sealObject();
        }
    }

    @Override
    public String getClassName() {
        return "Map";
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
                    NativeMap nm = new NativeMap();
                    nm.instanceOfMap = true;
                    if (args.length > 0) {
                        loadFromIterable(cx, scope, nm, key(args));
                    }
                    return nm;
                }
                throw ScriptRuntime.typeErrorById("msg.no.new", "Map");
            case Id_set:
                return realThis(thisObj, f)
                        .js_set(key(args), args.length > 1 ? args[1] : Undefined.instance);
            case Id_delete:
                return realThis(thisObj, f).js_delete(key(args));
            case Id_get:
                return realThis(thisObj, f).js_get(key(args));
            case Id_has:
                return realThis(thisObj, f).js_has(key(args));
            case Id_clear:
                return realThis(thisObj, f).js_clear();
            case Id_keys:
                return realThis(thisObj, f).js_iterator(scope, NativeCollectionIterator.Type.KEYS);
            case Id_values:
                return realThis(thisObj, f)
                        .js_iterator(scope, NativeCollectionIterator.Type.VALUES);
            case Id_entries:
                return realThis(thisObj, f).js_iterator(scope, NativeCollectionIterator.Type.BOTH);
            case Id_forEach:
                return realThis(thisObj, f)
                        .js_forEach(
                                cx,
                                scope,
                                args.length > 0 ? args[0] : Undefined.instance,
                                args.length > 1 ? args[1] : Undefined.instance);
            case SymbolId_getSize:
                return realThis(thisObj, f).js_getSize();
        }
        throw new IllegalArgumentException("Map.prototype has no method: " + f.getFunctionName());
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

    private Object js_delete(Object arg) {
        return Boolean.valueOf(entries.deleteEntry(arg));
    }

    private Object js_get(Object arg) {
        final Hashtable.Entry entry = entries.getEntry(arg);
        if (entry == null) {
            return Undefined.instance;
        }
        return entry.value;
    }

    private Object js_has(Object arg) {
        return Boolean.valueOf(entries.has(arg));
    }

    private Object js_getSize() {
        return Integer.valueOf(entries.size());
    }

    private Object js_iterator(Scriptable scope, NativeCollectionIterator.Type type) {
        return new NativeCollectionIterator(scope, ITERATOR_TAG, type, entries.iterator());
    }

    private Object js_clear() {
        entries.clear();
        return Undefined.instance;
    }

    private Object js_forEach(Context cx, Scriptable scope, Object arg1, Object arg2) {
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
                thisObj = scope;
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
    static void loadFromIterable(Context cx, Scriptable scope, ScriptableObject map, Object arg1) {
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
        final Callable set = ScriptRuntime.getPropFunctionAndThis(proto, "set", cx, scope);
        ScriptRuntime.lastStoredScriptable(cx);

        ScriptRuntime.loadFromIterable(
                cx,
                scope,
                arg1,
                (key, value) -> set.call(cx, scope, map, new Object[] {key, value}));
    }

    private static NativeMap realThis(Scriptable thisObj, IdFunctionObject f) {
        final NativeMap nm = ensureType(thisObj, NativeMap.class, f);
        if (!nm.instanceOfMap) {
            // Check for "Map internal data tag"
            throw ScriptRuntime.typeErrorById("msg.incompat.call", f.getFunctionName());
        }

        return nm;
    }

    @Override
    protected void initPrototypeId(int id) {
        switch (id) {
            case SymbolId_getSize:
                initPrototypeMethod(MAP_TAG, id, NativeSet.GETSIZE, "get size", 0);
                return;
            case SymbolId_toStringTag:
                initPrototypeValue(
                        SymbolId_toStringTag,
                        SymbolKey.TO_STRING_TAG,
                        getClassName(),
                        DONTENUM | READONLY);
                return;
                // fallthrough
        }

        String s, fnName = null;
        int arity;
        switch (id) {
            case Id_constructor:
                arity = 0;
                s = "constructor";
                break;
            case Id_set:
                arity = 2;
                s = "set";
                break;
            case Id_get:
                arity = 1;
                s = "get";
                break;
            case Id_delete:
                arity = 1;
                s = "delete";
                break;
            case Id_has:
                arity = 1;
                s = "has";
                break;
            case Id_clear:
                arity = 0;
                s = "clear";
                break;
            case Id_keys:
                arity = 0;
                s = "keys";
                break;
            case Id_values:
                arity = 0;
                s = "values";
                break;
            case Id_entries:
                arity = 0;
                s = "entries";
                break;
            case Id_forEach:
                arity = 1;
                s = "forEach";
                break;
            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(MAP_TAG, id, s, fnName, arity);
    }

    @Override
    protected int findPrototypeId(Symbol k) {
        if (NativeSet.GETSIZE.equals(k)) {
            return SymbolId_getSize;
        }
        if (SymbolKey.ITERATOR.equals(k)) {
            // ECMA spec says that the "Symbol.iterator" property of the prototype has the
            // "same value" as the "entries" property. We implement this by returning the
            // ID of "entries" when the iterator symbol is accessed.
            return Id_entries;
        }
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
            case "set":
                id = Id_set;
                break;
            case "get":
                id = Id_get;
                break;
            case "delete":
                id = Id_delete;
                break;
            case "has":
                id = Id_has;
                break;
            case "clear":
                id = Id_clear;
                break;
            case "keys":
                id = Id_keys;
                break;
            case "values":
                id = Id_values;
                break;
            case "entries":
                id = Id_entries;
                break;
            case "forEach":
                id = Id_forEach;
                break;
            default:
                id = 0;
                break;
        }
        return id;
    }

    // Note that "SymbolId_iterator" is not present here. That's because the spec
    // requires that it be the same value as the "entries" prototype property.
    private static final int Id_constructor = 1,
            Id_set = 2,
            Id_get = 3,
            Id_delete = 4,
            Id_has = 5,
            Id_clear = 6,
            Id_keys = 7,
            Id_values = 8,
            Id_entries = 9,
            Id_forEach = 10,
            SymbolId_getSize = 11,
            SymbolId_toStringTag = 12,
            MAX_PROTOTYPE_ID = SymbolId_toStringTag;

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
