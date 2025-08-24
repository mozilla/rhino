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
        // Special handling of "negative zero" from the spec.
        Object key = arg;
        if ((key instanceof Number) && ((Number) key).doubleValue() == ScriptRuntime.negativeZero) {
            key = ScriptRuntime.zeroObj;
        }
        return entries.has(key);
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

        NativeSet result = (NativeSet) cx.newObject(scope, CLASS_NAME);
        result.instanceOfSet = true;

        // Check if other is a Set-like object with size, has, and keys
        Scriptable scriptable = ScriptableObject.ensureScriptable(otherObj);
        Object sizeVal = ScriptableObject.getProperty(scriptable, "size");
        Object hasVal = ScriptableObject.getProperty(scriptable, "has");
        Object keysVal = ScriptableObject.getProperty(scriptable, "keys");

        if (sizeVal != Scriptable.NOT_FOUND
                && hasVal != Scriptable.NOT_FOUND
                && keysVal != Scriptable.NOT_FOUND) {
            // Set-like object path (Set, Map, etc.)
            return js_intersectionSetLike(cx, scope, otherObj, result, sizeVal, hasVal, keysVal);
        } else {
            // Fallback to generic iterable (arrays, strings, etc.)
            return js_intersectionIterable(cx, scope, otherObj, result);
        }
    }

    private Object js_intersectionSetLike(
            Context cx,
            Scriptable scope,
            Object otherObj,
            NativeSet result,
            Object sizeVal,
            Object hasVal,
            Object keysVal) {
        // Validate has and keys are callable
        if (!(hasVal instanceof Callable)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.isnt.function", "has", ScriptRuntime.typeof(hasVal));
        }
        if (!(keysVal instanceof Callable)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.isnt.function", "keys", ScriptRuntime.typeof(keysVal));
        }

        Callable hasMethod = (Callable) hasVal;
        Callable keysMethod = (Callable) keysVal;

        // ES2025: Compare sizes to determine iteration strategy
        double otherSizeDouble = ScriptRuntime.toNumber(sizeVal);
        if (Double.isNaN(otherSizeDouble)) {
            throw ScriptRuntime.typeError("size is not a number");
        }
        int otherSize =
                Double.isInfinite(otherSizeDouble)
                        ? Integer.MAX_VALUE
                        : (int) Math.floor(otherSizeDouble);
        int thisSize = entries.size();

        if (thisSize <= otherSize) {
            // When this.size <= other.size: iterate through this, call other.has()
            for (Hashtable.Entry entry : entries) {
                Object key = entry.key;
                Object inOther = callHas(cx, scope, otherObj, hasMethod, key);
                if (ScriptRuntime.toBoolean(inOther)) {
                    result.js_add(key);
                }
            }
        } else {
            // When this.size > other.size: iterate through other.keys(), call this.has()
            Object iterator =
                    ScriptRuntime.callIterator(
                            keysMethod.call(
                                    cx,
                                    scope,
                                    ScriptableObject.ensureScriptable(otherObj),
                                    ScriptRuntime.emptyArgs),
                            cx,
                            scope);
            try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, iterator)) {
                for (Object key : it) {
                    if (js_has(key) == Boolean.TRUE) {
                        result.js_add(key);
                    }
                }
            }
        }

        return result;
    }

    private Object js_intersectionIterable(
            Context cx, Scriptable scope, Object otherObj, NativeSet result) {
        // For generic iterables, iterate through all values and check if they exist in this set
        Object iterator = ScriptRuntime.callIterator(otherObj, cx, scope);

        try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, iterator)) {
            for (Object value : it) {
                if (js_has(value) == Boolean.TRUE) {
                    result.js_add(value);
                }
            }
        }

        return result;
    }

    private Object js_union(Context cx, Scriptable scope, Object[] args) {
        Object otherObj = args.length > 0 ? args[0] : Undefined.instance;

        NativeSet result = (NativeSet) cx.newObject(scope, CLASS_NAME);
        result.instanceOfSet = true;

        // Add all elements from this set
        for (Hashtable.Entry entry : entries) {
            result.js_add(entry.key);
        }

        // Check if other is a Set-like object (has size, has, AND keys)
        Scriptable scriptable = ScriptableObject.ensureScriptable(otherObj);
        Object sizeVal = ScriptableObject.getProperty(scriptable, "size");
        Object hasVal = ScriptableObject.getProperty(scriptable, "has");
        Object keysVal = ScriptableObject.getProperty(scriptable, "keys");

        if (sizeVal != Scriptable.NOT_FOUND
                && hasVal != Scriptable.NOT_FOUND
                && keysVal != Scriptable.NOT_FOUND) {
            // Validate keys is callable
            if (!(keysVal instanceof Callable)) {
                throw ScriptRuntime.typeErrorById(
                        "msg.isnt.function", "keys", ScriptRuntime.typeof(keysVal));
            }
            // Set-like object - use keys method
            Callable keysMethod = (Callable) keysVal;
            Object iterator =
                    ScriptRuntime.callIterator(
                            keysMethod.call(cx, scope, scriptable, ScriptRuntime.emptyArgs),
                            cx,
                            scope);
            try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, iterator)) {
                for (Object key : it) {
                    result.js_add(key);
                }
            }
        } else {
            // Generic iterable (including arrays) - use Symbol.iterator
            Object iterator = ScriptRuntime.callIterator(otherObj, cx, scope);
            try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, iterator)) {
                for (Object value : it) {
                    result.js_add(value);
                }
            }
        }

        return result;
    }

    private Object js_difference(Context cx, Scriptable scope, Object[] args) {
        Object otherObj = args.length > 0 ? args[0] : Undefined.instance;

        NativeSet result = (NativeSet) cx.newObject(scope, CLASS_NAME);
        result.instanceOfSet = true;

        // Check if other is a Set-like object with size, has, and keys
        Scriptable scriptable = ScriptableObject.ensureScriptable(otherObj);
        Object sizeVal = ScriptableObject.getProperty(scriptable, "size");
        Object hasVal = ScriptableObject.getProperty(scriptable, "has");
        Object keysVal = ScriptableObject.getProperty(scriptable, "keys");

        if (sizeVal != Scriptable.NOT_FOUND
                && hasVal != Scriptable.NOT_FOUND
                && keysVal != Scriptable.NOT_FOUND) {
            // Set-like object path with size optimization
            return js_differenceSetLike(cx, scope, otherObj, result, sizeVal, hasVal, keysVal);
        } else {
            // Generic iterable path - no size optimization possible
            return js_differenceIterable(cx, scope, otherObj, result);
        }
    }

    private Object js_differenceSetLike(
            Context cx,
            Scriptable scope,
            Object otherObj,
            NativeSet result,
            Object sizeVal,
            Object hasVal,
            Object keysVal) {
        // Validate has and keys are callable
        if (!(hasVal instanceof Callable)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.isnt.function", "has", ScriptRuntime.typeof(hasVal));
        }
        if (!(keysVal instanceof Callable)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.isnt.function", "keys", ScriptRuntime.typeof(keysVal));
        }

        Callable hasMethod = (Callable) hasVal;
        Callable keysMethod = (Callable) keysVal;

        int otherSize = ScriptRuntime.toInt32(sizeVal);
        int thisSize = entries.size();

        // According to the spec and test converts-negative-zero.js:
        // When this.size > other.size, we should iterate through other.keys()
        // and remove matching elements, NOT call other.has()
        if (thisSize > otherSize) {

            // First, add all elements from this set
            for (Hashtable.Entry entry : entries) {
                result.js_add(entry.key);
            }

            // Then iterate through other and remove matching elements
            Object iterator =
                    ScriptRuntime.callIterator(
                            keysMethod.call(
                                    cx,
                                    scope,
                                    ScriptableObject.ensureScriptable(otherObj),
                                    ScriptRuntime.emptyArgs),
                            cx,
                            scope);
            try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, iterator)) {
                for (Object key : it) {
                    // Convert -0 to +0 as the spec requires
                    if (key instanceof Number
                            && ((Number) key).doubleValue() == ScriptRuntime.negativeZero) {
                        key = ScriptRuntime.zeroObj;
                    }
                    result.js_delete(key);
                }
            }
        } else {
            // When this.size <= other.size, iterate through this and check with has()

            for (Hashtable.Entry entry : entries) {
                Object key = entry.key;
                Object inOther = callHas(cx, scope, otherObj, hasMethod, key);
                if (!ScriptRuntime.toBoolean(inOther)) {
                    result.js_add(key);
                }
            }
        }

        return result;
    }

    private Object js_differenceIterable(
            Context cx, Scriptable scope, Object otherObj, NativeSet result) {
        // First, add all elements from this set
        for (Hashtable.Entry entry : entries) {
            result.js_add(entry.key);
        }

        // Then iterate through other and remove matching elements
        Object iterator = ScriptRuntime.callIterator(otherObj, cx, scope);
        try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, iterator)) {
            for (Object value : it) {
                // Convert -0 to +0 as the spec requires
                if (value instanceof Number
                        && ((Number) value).doubleValue() == ScriptRuntime.negativeZero) {
                    value = ScriptRuntime.zeroObj;
                }
                result.js_delete(value);
            }
        }

        return result;
    }

    private Object js_symmetricDifference(Context cx, Scriptable scope, Object[] args) {
        Object otherObj = args.length > 0 ? args[0] : Undefined.instance;

        NativeSet result = (NativeSet) cx.newObject(scope, CLASS_NAME);
        result.instanceOfSet = true;

        // Check if other is a Set-like object
        Scriptable scriptable = ScriptableObject.ensureScriptable(otherObj);
        Object hasVal = ScriptableObject.getProperty(scriptable, "has");
        Object keysVal = ScriptableObject.getProperty(scriptable, "keys");

        // Check if other is a Set-like object (must have all three: size, has, keys)
        Object sizeVal = ScriptableObject.getProperty(scriptable, "size");
        
        if (sizeVal != Scriptable.NOT_FOUND
                && hasVal != Scriptable.NOT_FOUND
                && keysVal != Scriptable.NOT_FOUND) {
            // Validate has and keys are callable
            if (!(hasVal instanceof Callable)) {
                throw ScriptRuntime.typeErrorById(
                        "msg.isnt.function", "has", ScriptRuntime.typeof(hasVal));
            }
            if (!(keysVal instanceof Callable)) {
                throw ScriptRuntime.typeErrorById(
                        "msg.isnt.function", "keys", ScriptRuntime.typeof(keysVal));
            }
            
            // Set-like object path
            Callable hasMethod = (Callable) hasVal;
            Callable keysMethod = (Callable) keysVal;

            // Add elements from this that are not in other
            for (Hashtable.Entry entry : entries) {
                Object key = entry.key;
                Object inOther = callHas(cx, scope, otherObj, hasMethod, key);
                if (!ScriptRuntime.toBoolean(inOther)) {
                    result.js_add(key);
                }
            }

            // Add elements from other that are not in this
            Object iterator =
                    ScriptRuntime.callIterator(
                            keysMethod.call(cx, scope, scriptable, ScriptRuntime.emptyArgs),
                            cx,
                            scope);
            try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, iterator)) {
                for (Object key : it) {
                    if (js_has(key) != Boolean.TRUE) {
                        result.js_add(key);
                    }
                }
            }
        } else {
            // Per spec, reject non-Set-like objects
            throw ScriptRuntime.typeError("Set methods require Set-like objects");
        }

        return result;
    }

    private Object js_isSubsetOf(Context cx, Scriptable scope, Object[] args) {
        Object otherObj = args.length > 0 ? args[0] : Undefined.instance;

        // Check if other is a Set-like object with has
        Scriptable scriptable = ScriptableObject.ensureScriptable(otherObj);
        Object hasVal = ScriptableObject.getProperty(scriptable, "has");

        if (hasVal != Scriptable.NOT_FOUND && hasVal instanceof Callable) {
            // Set-like object - use has method for efficiency
            Callable hasMethod = (Callable) hasVal;

            // Check size optimization if available
            Object sizeVal = ScriptableObject.getProperty(scriptable, "size");
            if (sizeVal != Scriptable.NOT_FOUND) {
                int otherSize = ScriptRuntime.toInt32(sizeVal);
                int thisSize = entries.size();
                // If this set is larger than other, it cannot be a subset
                if (thisSize > otherSize) {
                    return Boolean.FALSE;
                }
            }

            // Check if all elements of this are in other
            for (Hashtable.Entry entry : entries) {
                Object key = entry.key;
                Object inOther = callHas(cx, scope, otherObj, hasMethod, key);
                if (!ScriptRuntime.toBoolean(inOther)) {
                    return Boolean.FALSE;
                }
            }
        } else {
            // Per spec, reject non-Set-like objects
            throw ScriptRuntime.typeError("Set methods require Set-like objects");
        }

        return Boolean.TRUE;
    }

    private Object js_isSupersetOf(Context cx, Scriptable scope, Object[] args) {
        Object otherObj = args.length > 0 ? args[0] : Undefined.instance;

        // Check if other is a Set-like object
        Scriptable scriptable = ScriptableObject.ensureScriptable(otherObj);
        Object sizeVal = ScriptableObject.getProperty(scriptable, "size");
        Object hasVal = ScriptableObject.getProperty(scriptable, "has");
        Object keysVal = ScriptableObject.getProperty(scriptable, "keys");

        if (sizeVal != Scriptable.NOT_FOUND
                && hasVal != Scriptable.NOT_FOUND
                && keysVal != Scriptable.NOT_FOUND) {
            // Validate keys is callable
            if (!(keysVal instanceof Callable)) {
                throw ScriptRuntime.typeErrorById(
                        "msg.isnt.function", "keys", ScriptRuntime.typeof(keysVal));
            }
            
            // Iterate through other.keys() and check if all elements are in this
            Callable keysMethod = (Callable) keysVal;
            Object iterator = ScriptRuntime.callIterator(
                    keysMethod.call(cx, scope, scriptable, ScriptRuntime.emptyArgs),
                    cx,
                    scope);
            try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, iterator)) {
                for (Object value : it) {
                    if (js_has(value) != Boolean.TRUE) {
                        return Boolean.FALSE;
                    }
                }
            }
            return Boolean.TRUE;
        } else {
            // Per spec, reject non-Set-like objects
            throw ScriptRuntime.typeError("Set methods require Set-like objects");
        }
    }

    private Object js_isDisjointFrom(Context cx, Scriptable scope, Object[] args) {
        Object otherObj = args.length > 0 ? args[0] : Undefined.instance;

        // Check if other is a Set-like object
        Scriptable scriptable = ScriptableObject.ensureScriptable(otherObj);
        Object sizeVal = ScriptableObject.getProperty(scriptable, "size");
        Object hasVal = ScriptableObject.getProperty(scriptable, "has");
        Object keysVal = ScriptableObject.getProperty(scriptable, "keys");

        if (sizeVal != Scriptable.NOT_FOUND
                && hasVal != Scriptable.NOT_FOUND
                && keysVal != Scriptable.NOT_FOUND
                && hasVal instanceof Callable
                && keysVal instanceof Callable) {
            // Set-like object with size optimization
            Callable hasMethod = (Callable) hasVal;
            Callable keysMethod = (Callable) keysVal;
            int otherSize = ScriptRuntime.toInt32(sizeVal);
            int thisSize = entries.size();

            if (thisSize <= otherSize) {
                // Iterate through this set
                for (Hashtable.Entry entry : entries) {
                    Object key = entry.key;
                    Object inOther = callHas(cx, scope, otherObj, hasMethod, key);
                    if (ScriptRuntime.toBoolean(inOther)) {
                        return Boolean.FALSE;
                    }
                }
            } else {
                // Iterate through other
                Object iterator =
                        ScriptRuntime.callIterator(
                                keysMethod.call(cx, scope, scriptable, ScriptRuntime.emptyArgs),
                                cx,
                                scope);
                try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, iterator)) {
                    for (Object key : it) {
                        if (js_has(key) == Boolean.TRUE) {
                            return Boolean.FALSE;
                        }
                    }
                }
            }
        } else {
            // Per spec, reject non-Set-like objects
            throw ScriptRuntime.typeError("Set methods require Set-like objects");
        }

        return Boolean.TRUE;
    }

    // Helper method for Set operations

    private static Object callHas(
            Context cx, Scriptable scope, Object obj, Object hasMethod, Object key) {
        return ((Callable) hasMethod)
                .call(cx, scope, ScriptableObject.ensureScriptable(obj), new Object[] {key});
    }
}
