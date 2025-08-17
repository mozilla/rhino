/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

public class NativeSet extends ScriptableObject {
    private static final long serialVersionUID = -8442212766987072986L;
    private static final String CLASS_NAME = "Set";
    static final String ITERATOR_TAG = "Set Iterator";

    static final SymbolKey GETSIZE = new SymbolKey("[Symbol.getSize]");

    private final Hashtable entries = new Hashtable();

    private boolean instanceOfSet = false;

    static Object init(Context cx, Scriptable scope, boolean sealed) {
        LambdaConstructor constructor =
                new LambdaConstructor(
                        scope,
                        CLASS_NAME,
                        0,
                        LambdaConstructor.CONSTRUCTOR_NEW,
                        NativeSet::jsConstructor);
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
        constructor.definePrototypeMethod(
                scope,
                "clear",
                0,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        realThis(thisObj, "clear").js_clear(),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "values",
                0,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        realThis(thisObj, "values")
                                .js_iterator(scope, NativeCollectionIterator.Type.VALUES),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeAlias("values", "keys", DONTENUM | READONLY);
        constructor.definePrototypeAlias("values", SymbolKey.ITERATOR, DONTENUM);

        constructor.definePrototypeMethod(
                scope,
                "forEach",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        realThis(thisObj, "forEach")
                                .js_forEach(
                                        lcx,
                                        lscope,
                                        NativeMap.key(args),
                                        args.length > 1 ? args[1] : Undefined.instance),
                DONTENUM,
                DONTENUM | READONLY);

        constructor.definePrototypeMethod(
                scope,
                "entries",
                0,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        realThis(thisObj, "entries")
                                .js_iterator(scope, NativeCollectionIterator.Type.BOTH),
                DONTENUM,
                DONTENUM | READONLY);

        // ES2025 Set methods
        constructor.definePrototypeMethod(
                scope,
                "intersection",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        realThis(thisObj, "intersection").js_intersection(lcx, lscope, args),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "union",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        realThis(thisObj, "union").js_union(lcx, lscope, args),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "difference",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        realThis(thisObj, "difference").js_difference(lcx, lscope, args),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "symmetricDifference",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        realThis(thisObj, "symmetricDifference")
                                .js_symmetricDifference(lcx, lscope, args),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "isSubsetOf",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        realThis(thisObj, "isSubsetOf").js_isSubsetOf(lcx, lscope, args),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "isSupersetOf",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        realThis(thisObj, "isSupersetOf").js_isSupersetOf(lcx, lscope, args),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "isDisjointFrom",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        realThis(thisObj, "isDisjointFrom").js_isDisjointFrom(lcx, lscope, args),
                DONTENUM,
                DONTENUM | READONLY);

        // The spec requires very specific handling of the "size" prototype
        // property that's not like other things that we already do.
        ScriptableObject desc = (ScriptableObject) cx.newObject(scope);
        desc.put("enumerable", desc, Boolean.FALSE);
        desc.put("configurable", desc, Boolean.TRUE);
        LambdaFunction sizeFunc =
                new LambdaFunction(
                        scope,
                        "get size",
                        0,
                        (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                                realThis(thisObj, "size").js_getSize());
        sizeFunc.setPrototypeProperty(Undefined.instance);
        desc.put("get", desc, sizeFunc);
        constructor.definePrototypeProperty(cx, "size", desc);
        constructor.definePrototypeProperty(cx, NativeSet.GETSIZE, desc);

        constructor.definePrototypeProperty(
                SymbolKey.TO_STRING_TAG, CLASS_NAME, DONTENUM | READONLY);

        ScriptRuntimeES6.addSymbolSpecies(cx, scope, constructor);
        if (sealed) {
            constructor.sealObject();
            ((ScriptableObject) constructor.getPrototypeProperty()).sealObject();
        }
        return constructor;
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    private static Scriptable jsConstructor(Context cx, Scriptable scope, Object[] args) {
        NativeSet ns = new NativeSet();
        ns.instanceOfSet = true;
        if (args.length > 0) {
            loadFromIterable(cx, scope, ns, NativeMap.key(args));
        }
        return ns;
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
        return entries.deleteEntry(arg);
    }

    private Object js_has(Object arg) {
        return entries.has(arg);
    }

    private Object js_clear() {
        entries.clear();
        return Undefined.instance;
    }

    private Object js_getSize() {
        return entries.size();
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
        var addCall = ScriptRuntime.getPropAndThis(dummy.getPrototype(), "add", cx, scope);
        Callable add = addCall.getCallable();

        // Finally, run through all the iterated values and add them!
        try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, ito)) {
            for (Object val : it) {
                final Object finalVal = val == Scriptable.NOT_FOUND ? Undefined.instance : val;
                add.call(cx, scope, set, new Object[] {finalVal});
            }
        }
    }

    private static NativeSet realThis(Scriptable thisObj, String name) {
        NativeSet ns = LambdaConstructor.convertThisObject(thisObj, NativeSet.class);
        if (!ns.instanceOfSet) {
            // If we get here, then this object doesn't have the "Set internal data slot."
            throw ScriptRuntime.typeErrorById("msg.incompat.call", name);
        }
        return ns;
    }

    // ES2025 Set Methods Implementation

    private Object js_intersection(Context cx, Scriptable scope, Object[] args) {
        Object otherObj = args.length > 0 ? args[0] : Undefined.instance;
        Object sizeVal = getSize(cx, scope, otherObj);

        // Always validate that keys and has are callable by calling the validation methods
        getKeysMethod(cx, scope, otherObj);
        Object hasMethod = getHas(cx, scope, otherObj);

        NativeSet result = (NativeSet) cx.newObject(scope, CLASS_NAME);
        result.instanceOfSet = true;

        // If other is smaller than this, iterate through other
        int otherSize = ScriptRuntime.toInt32(sizeVal);
        int thisSize = entries.size();

        if (otherSize < thisSize) {
            // Iterate through other and check if each item is in this
            Object iterator = ScriptRuntime.callIterator(getKeys(cx, scope, otherObj), cx, scope);
            try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, iterator)) {
                for (Object key : it) {
                    if (js_has(key) == Boolean.TRUE) {
                        result.js_add(key);
                    }
                }
            }
        } else {
            // Iterate through this and check if each item is in other
            for (Hashtable.Entry entry : entries) {
                Object key = entry.key;
                Object inOther = callHas(cx, scope, otherObj, hasMethod, key);
                if (ScriptRuntime.toBoolean(inOther)) {
                    result.js_add(key);
                }
            }
        }

        return result;
    }

    private Object js_union(Context cx, Scriptable scope, Object[] args) {
        Object otherObj = args.length > 0 ? args[0] : Undefined.instance;

        // Always validate that keys is callable
        getKeysMethod(cx, scope, otherObj);

        NativeSet result = (NativeSet) cx.newObject(scope, CLASS_NAME);
        result.instanceOfSet = true;

        // Add all elements from this set
        for (Hashtable.Entry entry : entries) {
            result.js_add(entry.key);
        }

        // Add all elements from other
        Object iterator = ScriptRuntime.callIterator(getKeys(cx, scope, otherObj), cx, scope);
        try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, iterator)) {
            for (Object key : it) {
                result.js_add(key);
            }
        }

        return result;
    }

    private Object js_difference(Context cx, Scriptable scope, Object[] args) {
        Object otherObj = args.length > 0 ? args[0] : Undefined.instance;

        // Always validate that has is callable
        Object hasMethod = getHas(cx, scope, otherObj);

        NativeSet result = (NativeSet) cx.newObject(scope, CLASS_NAME);
        result.instanceOfSet = true;

        // Add elements from this that are not in other
        for (Hashtable.Entry entry : entries) {
            Object key = entry.key;
            Object inOther = callHas(cx, scope, otherObj, hasMethod, key);
            if (!ScriptRuntime.toBoolean(inOther)) {
                result.js_add(key);
            }
        }

        return result;
    }

    private Object js_symmetricDifference(Context cx, Scriptable scope, Object[] args) {
        Object otherObj = args.length > 0 ? args[0] : Undefined.instance;

        // Always validate that keys and has are callable
        getKeysMethod(cx, scope, otherObj);
        Object hasMethod = getHas(cx, scope, otherObj);

        NativeSet result = (NativeSet) cx.newObject(scope, CLASS_NAME);
        result.instanceOfSet = true;

        // Add elements from this that are not in other
        for (Hashtable.Entry entry : entries) {
            Object key = entry.key;
            Object inOther = callHas(cx, scope, otherObj, hasMethod, key);
            if (!ScriptRuntime.toBoolean(inOther)) {
                result.js_add(key);
            }
        }

        // Add elements from other that are not in this
        Object iterator = ScriptRuntime.callIterator(getKeys(cx, scope, otherObj), cx, scope);
        try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, iterator)) {
            for (Object key : it) {
                if (js_has(key) != Boolean.TRUE) {
                    result.js_add(key);
                }
            }
        }

        return result;
    }

    private Object js_isSubsetOf(Context cx, Scriptable scope, Object[] args) {
        Object otherObj = args.length > 0 ? args[0] : Undefined.instance;

        // Always validate that has is callable
        Object hasMethod = getHas(cx, scope, otherObj);

        Object sizeVal = getSize(cx, scope, otherObj);
        int otherSize = ScriptRuntime.toInt32(sizeVal);
        int thisSize = entries.size();

        // If this set is larger than other, it cannot be a subset
        if (thisSize > otherSize) {
            return Boolean.FALSE;
        }

        // Check if all elements of this are in other
        for (Hashtable.Entry entry : entries) {
            Object key = entry.key;
            Object inOther = callHas(cx, scope, otherObj, hasMethod, key);
            if (!ScriptRuntime.toBoolean(inOther)) {
                return Boolean.FALSE;
            }
        }

        return Boolean.TRUE;
    }

    private Object js_isSupersetOf(Context cx, Scriptable scope, Object[] args) {
        Object otherObj = args.length > 0 ? args[0] : Undefined.instance;

        // Always validate that keys is callable
        getKeysMethod(cx, scope, otherObj);

        // Check if all elements of other are in this
        Object iterator = ScriptRuntime.callIterator(getKeys(cx, scope, otherObj), cx, scope);
        try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, iterator)) {
            for (Object key : it) {
                if (js_has(key) != Boolean.TRUE) {
                    return Boolean.FALSE;
                }
            }
        }

        return Boolean.TRUE;
    }

    private Object js_isDisjointFrom(Context cx, Scriptable scope, Object[] args) {
        Object otherObj = args.length > 0 ? args[0] : Undefined.instance;

        // Always validate that keys and has are callable
        getKeysMethod(cx, scope, otherObj);
        Object hasMethod = getHas(cx, scope, otherObj);

        Object sizeVal = getSize(cx, scope, otherObj);
        int otherSize = ScriptRuntime.toInt32(sizeVal);
        int thisSize = entries.size();

        if (otherSize < thisSize) {
            // Iterate through other
            Object iterator = ScriptRuntime.callIterator(getKeys(cx, scope, otherObj), cx, scope);
            try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, iterator)) {
                for (Object key : it) {
                    if (js_has(key) == Boolean.TRUE) {
                        return Boolean.FALSE;
                    }
                }
            }
        } else {
            // Iterate through this
            for (Hashtable.Entry entry : entries) {
                Object key = entry.key;
                Object inOther = callHas(cx, scope, otherObj, hasMethod, key);
                if (ScriptRuntime.toBoolean(inOther)) {
                    return Boolean.FALSE;
                }
            }
        }

        return Boolean.TRUE;
    }

    // Helper methods for Set operations

    private static Object getSize(Context cx, Scriptable scope, Object obj) {
        Object sizeVal =
                ScriptableObject.getProperty(ScriptableObject.ensureScriptable(obj), "size");
        if (sizeVal == Scriptable.NOT_FOUND) {
            throw ScriptRuntime.typeErrorById("msg.no.properties", ScriptRuntime.toString(obj));
        }
        return sizeVal;
    }

    private static Callable getKeysMethod(Context cx, Scriptable scope, Object obj) {
        Scriptable scriptable = ScriptableObject.ensureScriptable(obj);
        Object keysVal = ScriptableObject.getProperty(scriptable, "keys");
        if (keysVal == Scriptable.NOT_FOUND) {
            throw ScriptRuntime.typeErrorById("msg.no.properties", ScriptRuntime.toString(obj));
        }
        if (!(keysVal instanceof Callable)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.isnt.function", "keys", ScriptRuntime.typeof(keysVal));
        }
        return (Callable) keysVal;
    }

    private static Object getKeys(Context cx, Scriptable scope, Object obj) {
        Callable keysMethod = getKeysMethod(cx, scope, obj);
        Scriptable scriptable = ScriptableObject.ensureScriptable(obj);
        return keysMethod.call(cx, scope, scriptable, ScriptRuntime.emptyArgs);
    }

    private static Object getHas(Context cx, Scriptable scope, Object obj) {
        Scriptable scriptable = ScriptableObject.ensureScriptable(obj);
        Object hasVal = ScriptableObject.getProperty(scriptable, "has");
        if (hasVal == Scriptable.NOT_FOUND) {
            throw ScriptRuntime.typeErrorById("msg.no.properties", ScriptRuntime.toString(obj));
        }
        if (!(hasVal instanceof Callable)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.isnt.function", "has", ScriptRuntime.typeof(hasVal));
        }
        return hasVal;
    }

    private static Object callHas(
            Context cx, Scriptable scope, Object obj, Object hasMethod, Object key) {
        return ((Callable) hasMethod)
                .call(cx, scope, ScriptableObject.ensureScriptable(obj), new Object[] {key});
    }
}
