/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * The global Iterator constructor object as defined in ES2025.
 *
 * <p>This implements the Iterator constructor which serves as the base for iterator helper methods.
 * The constructor itself is not directly callable or constructable per spec.
 *
 * <p>See: https://tc39.es/proposal-iterator-helpers/#sec-iterator-constructor
 */
public final class NativeIteratorConstructor extends BaseFunction {
    private static final long serialVersionUID = 1L;

    private static final String ITERATOR_NAME = "Iterator";
    public static final String ITERATOR_PROTOTYPE_TAG = "IteratorPrototype";

    private NativeIteratorConstructor() {
        // Private constructor for singleton pattern
    }

    /**
     * Initialize the Iterator constructor in the given scope.
     *
     * @param scope the scope to initialize in
     * @param sealed whether to seal the objects
     */
    public static void init(ScriptableObject scope, boolean sealed) {
        Context cx = Context.getContext();
        NativeIteratorConstructor constructor = new NativeIteratorConstructor();
        constructor.setParentScope(scope);
        constructor.setPrototype(getFunctionPrototype(scope));

        // Create Iterator.prototype
        NativeObject prototype = new NativeObject();
        prototype.setParentScope(scope);
        prototype.setPrototype(getObjectPrototype(scope));

        // Define Symbol.toStringTag on Iterator.prototype
        prototype.defineProperty(
                SymbolKey.TO_STRING_TAG,
                ITERATOR_NAME,
                ScriptableObject.DONTENUM | ScriptableObject.READONLY);

        // Define Symbol.iterator on Iterator.prototype - returns this
        BaseFunction iteratorMethod =
                new BaseFunction() {
                    @Override
                    public Object call(
                            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                        // ES2025: %Iterator.prototype%[@@iterator] just returns this
                        return thisObj;
                    }

                    @Override
                    public String getFunctionName() {
                        return "[Symbol.iterator]";
                    }
                };
        prototype.defineProperty(SymbolKey.ITERATOR, iteratorMethod, ScriptableObject.DONTENUM);

        // Set up constructor properties per ES2025 spec
        int attrs =
                ScriptableObject.DONTENUM | ScriptableObject.READONLY | ScriptableObject.PERMANENT;
        constructor.defineProperty("prototype", prototype, attrs);
        constructor.defineProperty(
                "name", ITERATOR_NAME, ScriptableObject.DONTENUM | ScriptableObject.READONLY);
        constructor.defineProperty(
                "length",
                Integer.valueOf(0),
                ScriptableObject.DONTENUM | ScriptableObject.READONLY);

        // Define Iterator.from static method
        BaseFunction fromMethod =
                new BaseFunction() {
                    @Override
                    public Object call(
                            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                        return iteratorFrom(
                                cx, scope, args.length > 0 ? args[0] : Undefined.instance);
                    }

                    @Override
                    public String getFunctionName() {
                        return "from";
                    }

                    @Override
                    public int getLength() {
                        return 1;
                    }
                };
        constructor.defineProperty("from", fromMethod, ScriptableObject.DONTENUM);

        if (sealed) {
            constructor.sealObject();
            prototype.sealObject();
        }

        // TODO: Currently we don't define Iterator as a global property because it conflicts
        // with the legacy Iterator() function used for Java interop (NativeIterator).
        // Once we have a migration path, we should enable this:
        // ScriptableObject.defineProperty(
        //         scope, ITERATOR_NAME, constructor, ScriptableObject.DONTENUM);

        // Store prototype for later use by iterator helpers and ES6Iterator
        scope.associateValue(ITERATOR_PROTOTYPE_TAG, prototype);
    }

    @Override
    public String getClassName() {
        return ITERATOR_NAME;
    }

    @Override
    public String getFunctionName() {
        return ITERATOR_NAME;
    }

    /**
     * Iterator() called as a function - always throws per ES2025 spec.
     *
     * @throws EcmaError TypeError as Iterator is not callable
     */
    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        throw ScriptRuntime.typeErrorById("msg.not.ctor", ITERATOR_NAME);
    }

    /**
     * new Iterator() - always throws per ES2025 spec.
     *
     * @throws EcmaError TypeError as Iterator is not constructable
     */
    @Override
    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
        throw ScriptRuntime.typeErrorById("msg.not.ctor", ITERATOR_NAME);
    }

    /**
     * Get the Iterator prototype from the scope.
     *
     * @param scope the scope to look in
     * @return the Iterator.prototype object, or null if not initialized
     */
    public static Scriptable getIteratorPrototype(Scriptable scope) {
        Scriptable top = ScriptableObject.getTopLevelScope(scope);
        Object proto = ScriptableObject.getTopScopeValue(top, ITERATOR_PROTOTYPE_TAG);
        return proto instanceof Scriptable ? (Scriptable) proto : null;
    }

    /**
     * Implementation of Iterator.from(item) as defined in ES2025.
     *
     * @param cx the context
     * @param scope the scope
     * @param item the item to convert to an iterator
     * @return an iterator wrapping the item
     */
    private static Object iteratorFrom(Context cx, Scriptable scope, Object item) {
        // 1. If item is a string primitive, handle it specially (strings are iterable)
        if (item instanceof CharSequence) {
            // Convert string to object to get its iterator
            Object stringObj = ScriptRuntime.toObject(cx, scope, item);
            Object iteratorMethod =
                    ScriptableObject.getProperty((Scriptable) stringObj, SymbolKey.ITERATOR);
            if (iteratorMethod instanceof Callable) {
                Callable func = (Callable) iteratorMethod;
                Object iterator =
                        func.call(cx, scope, (Scriptable) stringObj, ScriptRuntime.emptyArgs);
                if (iterator instanceof Scriptable) {
                    return new IteratorWrapper(scope, (Scriptable) iterator);
                }
            }
        }

        // 2. If item is any other primitive (including null/undefined), throw TypeError
        if (!(item instanceof Scriptable)) {
            // This handles null, undefined, numbers, booleans, symbols, bigints
            throw ScriptRuntime.typeErrorById("msg.not.iterable", ScriptRuntime.toString(item));
        }

        Scriptable itemObj = (Scriptable) item;

        // 3. Get the @@iterator method
        Object iteratorMethod = ScriptableObject.getProperty(itemObj, SymbolKey.ITERATOR);

        // 4. Handle the iterator method
        if (iteratorMethod == Scriptable.NOT_FOUND) {
            // No Symbol.iterator property at all - treat as iterator-like
            return new IteratorWrapper(scope, itemObj);
        }

        if (iteratorMethod == null || Undefined.isUndefined(iteratorMethod)) {
            // Symbol.iterator is explicitly null or undefined - treat as iterator-like
            return new IteratorWrapper(scope, itemObj);
        }

        // Symbol.iterator exists and is not null/undefined
        if (!(iteratorMethod instanceof Callable)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.isnt.function",
                    SymbolKey.ITERATOR.toString(),
                    ScriptRuntime.typeof(iteratorMethod));
        }

        // 5. Call the iterator method
        Callable func = (Callable) iteratorMethod;
        Object iterator = func.call(cx, scope, itemObj, ScriptRuntime.emptyArgs);

        // 6. Check if result is an object
        if (!(iterator instanceof Scriptable)) {
            throw ScriptRuntime.typeErrorById("msg.iterator.primitive");
        }

        Scriptable iteratorObj = (Scriptable) iterator;

        // 7. Check if the iterator already inherits from Iterator.prototype
        Scriptable iteratorPrototype = getIteratorPrototype(scope);
        if (iteratorPrototype != null) {
            Scriptable proto = iteratorObj.getPrototype();
            while (proto != null) {
                if (proto == iteratorPrototype) {
                    // Already inherits from Iterator.prototype, return as-is
                    return iterator;
                }
                proto = proto.getPrototype();
            }
        }

        // 8. Wrap the iterator to inherit from Iterator.prototype
        return new IteratorWrapper(scope, iteratorObj);
    }
}
