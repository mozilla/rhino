/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

public class NativeSet extends IdScriptableObject {
    private static final long serialVersionUID = -8442212766987072986L;
    private static final Object SET_TAG = "Set";
    static final String ITERATOR_TAG = "Set Iterator";

    static final SymbolKey GETSIZE = new SymbolKey("[Symbol.getSize]");

    private final Hashtable entries = new Hashtable();

    private boolean instanceOfSet = false;

    static void init(Context cx, Scriptable scope, boolean sealed) {
        NativeSet obj = new NativeSet();
        IdFunctionObject constructor = obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, false);

        ScriptableObject desc = (ScriptableObject) cx.newObject(scope);
        desc.put("enumerable", desc, Boolean.FALSE);
        desc.put("configurable", desc, Boolean.TRUE);
        desc.put("get", desc, obj.get(GETSIZE, obj));
        obj.defineOwnProperty(cx, "size", desc);

        ScriptRuntimeES6.addSymbolSpecies(cx, scope, constructor);

        if (sealed) {
            obj.sealObject();
        }
    }

    @Override
    public String getClassName() {
        return "Set";
    }

    @Override
    public Object execIdCall(
            IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(SET_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        final int id = f.methodId();
        switch (id) {
            case Id_constructor:
                if (thisObj == null) {
                    NativeSet ns = new NativeSet();
                    ns.instanceOfSet = true;
                    if (args.length > 0) {
                        loadFromIterable(cx, scope, ns, NativeMap.key(args));
                    }
                    return ns;
                } else {
                    throw ScriptRuntime.typeErrorById("msg.no.new", "Set");
                }
            case Id_add:
                return realThis(thisObj, f).js_add(NativeMap.key(args));
            case Id_delete:
                return realThis(thisObj, f).js_delete(NativeMap.key(args));
            case Id_has:
                return realThis(thisObj, f).js_has(NativeMap.key(args));
            case Id_clear:
                return realThis(thisObj, f).js_clear();
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
                                NativeMap.key(args),
                                args.length > 1 ? args[1] : Undefined.instance);
            case SymbolId_getSize:
                return realThis(thisObj, f).js_getSize();
        }
        throw new IllegalArgumentException("Set.prototype has no method: " + f.getFunctionName());
    }

    private Object js_add(Object k) {
        // Special handling of "negative zero" from the spec.
        Object key = k;
        if ((key instanceof Number) && ((Number) key).doubleValue() == ScriptRuntime.negativeZero) {
            key = ScriptRuntime.zeroObj;
        }
        entries.put(key, key);
        return this;
    }

    private Object js_delete(Object arg) {
        return Boolean.valueOf(entries.deleteEntry(arg));
    }

    private Object js_has(Object arg) {
        return Boolean.valueOf(entries.has(arg));
    }

    private Object js_clear() {
        entries.clear();
        return Undefined.instance;
    }

    private Object js_getSize() {
        return Integer.valueOf(entries.size());
    }

    private Object js_iterator(Scriptable scope, NativeCollectionIterator.Type type) {
        return new NativeCollectionIterator(scope, ITERATOR_TAG, type, entries.iterator());
    }

    private Object js_forEach(Context cx, Scriptable scope, Object arg1, Object arg2) {
        if (!(arg1 instanceof Callable)) {
            throw ScriptRuntime.notFunctionError(arg1);
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
            f.call(cx, scope, thisObj, new Object[] {e.value, e.value, this});
        }
        return Undefined.instance;
    }

    /**
     * If an "iterable" object was passed to the constructor, there are many many things to do. This
     * is common code with NativeWeakSet.
     */
    static void loadFromIterable(Context cx, Scriptable scope, ScriptableObject set, Object arg1) {
        if ((arg1 == null) || Undefined.instance.equals(arg1)) {
            return;
        }

        // Call the "[Symbol.iterator]" property as a function.
        Object ito = ScriptRuntime.callIterator(arg1, cx, scope);
        if (Undefined.instance.equals(ito)) {
            // Per spec, ignore if the iterator returns undefined
            return;
        }

        // Find the "add" function of our own prototype, since it might have
        // been replaced. Since we're not fully constructed yet, create a dummy instance
        // so that we can get our own prototype.
        ScriptableObject dummy = ensureScriptableObject(cx.newObject(scope, set.getClassName()));
        final Callable add =
                ScriptRuntime.getPropFunctionAndThis(dummy.getPrototype(), "add", cx, scope);
        // Clean up the value left around by the previous function
        ScriptRuntime.lastStoredScriptable(cx);

        // Finally, run through all the iterated values and add them!
        try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, ito)) {
            for (Object val : it) {
                final Object finalVal = val == Scriptable.NOT_FOUND ? Undefined.instance : val;
                add.call(cx, scope, set, new Object[] {finalVal});
            }
        }
    }

    private static NativeSet realThis(Scriptable thisObj, IdFunctionObject f) {
        final NativeSet ns = ensureType(thisObj, NativeSet.class, f);
        if (!ns.instanceOfSet) {
            // If we get here, then this object doesn't have the "Set internal data slot."
            throw ScriptRuntime.typeErrorById("msg.incompat.call", f.getFunctionName());
        }

        return ns;
    }

    @Override
    protected void initPrototypeId(int id) {
        switch (id) {
            case SymbolId_getSize:
                initPrototypeMethod(SET_TAG, id, GETSIZE, "get size", 0);
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
            case Id_add:
                arity = 1;
                s = "add";
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
            case Id_entries:
                arity = 0;
                s = "entries";
                break;
            case Id_values:
                arity = 0;
                s = "values";
                break;
            case Id_forEach:
                arity = 1;
                s = "forEach";
                break;
            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(SET_TAG, id, s, fnName, arity);
    }

    @Override
    protected int findPrototypeId(Symbol k) {
        if (GETSIZE.equals(k)) {
            return SymbolId_getSize;
        }
        if (SymbolKey.ITERATOR.equals(k)) {
            return Id_values;
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
            case "add":
                id = Id_add;
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

    // Note that SymbolId_iterator is not present because it is required to have the
    // same value as the "values" entry.
    // Similarly, "keys" is supposed to have the same value as "values," which is why
    // both have the same ID.
    private static final int Id_constructor = 1,
            Id_add = 2,
            Id_delete = 3,
            Id_has = 4,
            Id_clear = 5,
            Id_keys = 6,
            Id_values = 6, // These are deliberately the same to match the spec
            Id_entries = 7,
            Id_forEach = 8,
            SymbolId_getSize = 9,
            SymbolId_toStringTag = 10,
            MAX_PROTOTYPE_ID = SymbolId_toStringTag;
}
