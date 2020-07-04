/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.Iterator;

public class NativeSet extends IdScriptableObject {
    private static final long serialVersionUID = -8442212766987072986L;
    private static final Object SET_TAG = "Set";
    static final String ITERATOR_TAG = "Set Iterator";

    static final SymbolKey GETSIZE = new SymbolKey("[Symbol.getSize]");

    private final Hashtable entries = new Hashtable();

    private boolean instanceOfSet = false;

    static void init(Context cx, Scriptable scope, boolean sealed)
    {
        NativeSet obj = new NativeSet();
        obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, false);

        ScriptableObject desc = (ScriptableObject)cx.newObject(scope);
        desc.put("enumerable", desc, Boolean.FALSE);
        desc.put("configurable", desc, Boolean.TRUE);
        desc.put("get", desc, obj.get(GETSIZE, obj));
        obj.defineOwnProperty(cx, "size", desc);

        if (sealed) {
            obj.sealObject();
        }
    }

    @Override
    public String getClassName() {
        return "Set";
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Scriptable thisObj, Object[] args)
    {
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
                        loadFromIterable(cx, scope, ns, args[0]);
                    }
                    return ns;
                } else {
                    throw ScriptRuntime.typeError1("msg.no.new", "Set");
                }
            case Id_add:
                return realThis(thisObj, f).js_add(args.length > 0 ? args[0] : Undefined.instance);
            case Id_delete:
                return realThis(thisObj, f).js_delete(args.length > 0 ? args[0] : Undefined.instance);
            case Id_has:
                return realThis(thisObj, f).js_has(args.length > 0 ? args[0] : Undefined.instance);
            case Id_clear:
                return realThis(thisObj, f).js_clear();
            case Id_values:
                return realThis(thisObj, f).js_iterator(scope, NativeCollectionIterator.Type.VALUES);
            case Id_entries:
                return realThis(thisObj, f).js_iterator(scope, NativeCollectionIterator.Type.BOTH);
            case Id_forEach:
                return realThis(thisObj, f).js_forEach(cx, scope,
                    args.length > 0 ? args[0] : Undefined.instance,
                    args.length > 1 ? args[1] : Undefined.instance);
            case SymbolId_getSize:
                return realThis(thisObj, f).js_getSize();
        }
        throw new IllegalArgumentException("Set.prototype has no method: " + f.getFunctionName());
    }

    private Object js_add(Object k)
    {
        // Special handling of "negative zero" from the spec.
        Object key = k;
        if ((key instanceof Number) &&
            ((Number)key).doubleValue() == ScriptRuntime.negativeZero) {
            key = ScriptRuntime.zeroObj;
        }
        entries.put(key, key);
        return this;
    }

    private Object js_delete(Object arg)
    {
        final Object ov = entries.delete(arg);
        return Boolean.valueOf(ov != null);
    }

    private Object js_has(Object arg)
    {
        return Boolean.valueOf(entries.has(arg));
    }

    private Object js_clear()
    {
        entries.clear();
        return Undefined.instance;
    }

    private Object js_getSize()
    {
        return Integer.valueOf(entries.size());
    }

    private Object js_iterator(Scriptable scope, NativeCollectionIterator.Type type)
    {
        return new NativeCollectionIterator(scope, ITERATOR_TAG, type, entries.iterator());
    }

    private Object js_forEach(Context cx, Scriptable scope, Object arg1, Object arg2)
    {
        if (!(arg1 instanceof Callable)) {
            throw ScriptRuntime.notFunctionError(arg1);
        }
        final Callable f = (Callable)arg1;

        boolean isStrict = cx.isStrictMode();
        Iterator<Hashtable.Entry> i = entries.iterator();
        while (i.hasNext()) {
            // Per spec must convert every time so that primitives are always regenerated...
            Scriptable thisObj = ScriptRuntime.toObjectOrNull(cx, arg2, scope);

            if (thisObj == null && !isStrict) {
                thisObj = scope;
            }
            if (thisObj == null) {
                thisObj = Undefined.SCRIPTABLE_UNDEFINED;
            }

            final Hashtable.Entry e = i.next();
            f.call(cx, scope, thisObj, new Object[] { e.value, e.value, this });
        }
        return Undefined.instance;
    }

    /**
     * If an "iterable" object was passed to the constructor, there are many many things
     * to do. This is common code with NativeWeakSet.
     */
    static void loadFromIterable(Context cx, Scriptable scope, ScriptableObject set, Object arg1)
    {
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
                add.call(cx, scope, set, new Object[]{finalVal});
            }
        }
    }

    private static NativeSet realThis(Scriptable thisObj, IdFunctionObject f)
    {
        if (thisObj == null) {
            throw incompatibleCallError(f);
        }
        try {
            final NativeSet ns = (NativeSet)thisObj;
            if (!ns.instanceOfSet) {
                // If we get here, then this object doesn't have the "Set internal data slot."
                throw incompatibleCallError(f);
            }
            return ns;
        } catch (ClassCastException cce) {
            throw incompatibleCallError(f);
        }
    }

    @Override
    protected void initPrototypeId(int id)
    {
        switch (id) {
            case SymbolId_getSize:
                initPrototypeMethod(SET_TAG, id, GETSIZE, "get size", 0);
                return;
            case SymbolId_toStringTag:
                initPrototypeValue(SymbolId_toStringTag, SymbolKey.TO_STRING_TAG,
                    getClassName(), DONTENUM | READONLY);
                return;
            // fallthrough
        }

        String s, fnName = null;
        int arity;
        switch (id) {
            case Id_constructor:       arity=0; s="constructor";       break;
            case Id_add:               arity=1; s="add";               break;
            case Id_delete:            arity=1; s="delete";            break;
            case Id_has:               arity=1; s="has";               break;
            case Id_clear:             arity=0; s="clear";             break;
            case Id_entries:           arity=0; s="entries";           break;
            case Id_values:            arity=0; s="values";            break;
            case Id_forEach:           arity=1; s="forEach";           break;
            default: throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(SET_TAG, id, s, fnName, arity);
    }

    @Override
    protected int findPrototypeId(Symbol k)
    {
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

// #string_id_map#

    @Override
    protected int findPrototypeId(String s)
    {
        int id;
// #generated# Last update: 2018-03-22 00:54:31 MDT
        L0: { id = 0; String X = null; int c;
            L: switch (s.length()) {
            case 3: c=s.charAt(0);
                if (c=='a') { if (s.charAt(2)=='d' && s.charAt(1)=='d') {id=Id_add; break L0;} }
                else if (c=='h') { if (s.charAt(2)=='s' && s.charAt(1)=='a') {id=Id_has; break L0;} }
                break L;
            case 4: X="keys";id=Id_keys; break L;
            case 5: X="clear";id=Id_clear; break L;
            case 6: c=s.charAt(0);
                if (c=='d') { X="delete";id=Id_delete; }
                else if (c=='v') { X="values";id=Id_values; }
                break L;
            case 7: c=s.charAt(0);
                if (c=='e') { X="entries";id=Id_entries; }
                else if (c=='f') { X="forEach";id=Id_forEach; }
                break L;
            case 11: X="constructor";id=Id_constructor; break L;
            }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
        return id;
    }

    // Note that SymbolId_iterator is not present because it is required to have the
    // same value as the "values" entry.
    // Similarly, "keys" is supposed to have the same value as "values," which is why
    // both have the same ID.
    private static final int
            Id_constructor = 1,
            Id_add = 2,
            Id_delete = 3,
            Id_has = 4,
            Id_clear = 5,
            Id_keys = 6,
            Id_values = 6,  // These are deliberately the same to match the spec
            Id_entries = 7,
            Id_forEach = 8,
            SymbolId_getSize = 9,
            SymbolId_toStringTag = 10,
            MAX_PROTOTYPE_ID = SymbolId_toStringTag;

// #/string_id_map#
}

