/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.ArrayList;
import java.util.List;

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
     * Initialize the Iterator constructor in the given scope and expose it globally.
     *
     * @param scope the scope to initialize in
     * @param sealed whether to seal the objects
     */
    public static void init(ScriptableObject scope, boolean sealed) {
        initInternal(scope, sealed, true);
    }

    /**
     * Initialize only the Iterator.prototype without exposing Iterator globally. This is used when
     * the legacy Iterator is active but we still want Iterator.prototype available for iterator
     * helpers.
     *
     * @param scope the scope to initialize in
     * @param sealed whether to seal the objects
     */
    public static void initPrototypeOnly(ScriptableObject scope, boolean sealed) {
        initInternal(scope, sealed, false);
    }

    private static void initInternal(
            ScriptableObject scope, boolean sealed, boolean exposeGlobally) {
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

        // Define Iterator.prototype.toArray method
        BaseFunction toArrayMethod =
                new BaseFunction() {
                    @Override
                    public Object call(
                            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                        // 1. Let O be the this value
                        // 2. Throw a TypeError if O is not an Object
                        if (thisObj == null) {
                            throw ScriptRuntime.typeErrorById(
                                    "msg.object.required", ScriptRuntime.typeof(thisObj));
                        }

                        // 3. Get the iterator using GetIteratorDirect(O)
                        // For ES2025, we expect thisObj to already be an iterator
                        Scriptable iterator = (Scriptable) thisObj;

                        // 4. Create an empty list called items
                        List<Object> items = new ArrayList<>();

                        // 5. Repeat
                        Object nextMethod = ScriptableObject.getProperty(iterator, "next");
                        if (!(nextMethod instanceof Callable)) {
                            throw ScriptRuntime.typeErrorById("msg.not.iterable", thisObj);
                        }

                        while (true) {
                            // Get the next value using IteratorStepValue()
                            Object iterResult =
                                    ((Callable) nextMethod)
                                            .call(cx, scope, iterator, ScriptRuntime.emptyArgs);

                            if (!(iterResult instanceof Scriptable)) {
                                throw ScriptRuntime.typeErrorById("msg.not.object", iterResult);
                            }

                            Object done =
                                    ScriptableObject.getProperty((Scriptable) iterResult, "done");
                            if (ScriptRuntime.toBoolean(done)) {
                                // If value is "done", return a new array created from items
                                return cx.newArray(scope, items.toArray());
                            }

                            Object value =
                                    ScriptableObject.getProperty((Scriptable) iterResult, "value");
                            // Append the value to items
                            items.add(value);
                        }
                    }

                    @Override
                    public String getFunctionName() {
                        return "toArray";
                    }

                    @Override
                    public int getLength() {
                        return 0;
                    }
                };
        prototype.defineProperty("toArray", toArrayMethod, ScriptableObject.DONTENUM);

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

        // Conditionally expose Iterator globally based on parameter
        if (exposeGlobally) {
            ScriptableObject.defineProperty(
                    scope, ITERATOR_NAME, constructor, ScriptableObject.DONTENUM);
        }

        // Always store prototype for later use by iterator helpers and ES6Iterator
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
        // Handle string primitives specially (they are iterable)
        if (item instanceof CharSequence) {
            return wrapStringIterator(cx, scope, item);
        }

        // Reject other primitives (null, undefined, numbers, booleans, symbols, bigints)
        if (!(item instanceof Scriptable)) {
            throw ScriptRuntime.typeErrorById("msg.not.iterable", ScriptRuntime.toString(item));
        }

        Scriptable itemObj = (Scriptable) item;
        Object iteratorMethod = ScriptableObject.getProperty(itemObj, SymbolKey.ITERATOR);

        // If no Symbol.iterator or it's null/undefined, treat as iterator-like object
        if (iteratorMethod == Scriptable.NOT_FOUND
                || iteratorMethod == null
                || Undefined.isUndefined(iteratorMethod)) {
            return new IteratorWrapper(itemObj, scope);
        }

        // Symbol.iterator exists - must be callable
        if (!(iteratorMethod instanceof Callable)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.isnt.function",
                    SymbolKey.ITERATOR.toString(),
                    ScriptRuntime.typeof(iteratorMethod));
        }

        // Call the iterator method
        Object iterator =
                ((Callable) iteratorMethod).call(cx, scope, itemObj, ScriptRuntime.emptyArgs);

        // Result must be an object
        if (!(iterator instanceof Scriptable)) {
            throw ScriptRuntime.typeErrorById("msg.iterator.primitive");
        }

        // Check if already inherits from Iterator.prototype
        if (isIteratorPrototypeInstance((Scriptable) iterator, scope)) {
            return iterator;
        }

        // Wrap the iterator to inherit from Iterator.prototype
        return new IteratorWrapper((Scriptable) iterator, scope);
    }

    /** Wraps a string primitive's iterator. */
    private static Object wrapStringIterator(Context cx, Scriptable scope, Object string) {
        Object stringObj = ScriptRuntime.toObject(cx, scope, string);
        Object iteratorMethod =
                ScriptableObject.getProperty((Scriptable) stringObj, SymbolKey.ITERATOR);

        if (iteratorMethod instanceof Callable) {
            Object iterator =
                    ((Callable) iteratorMethod)
                            .call(cx, scope, (Scriptable) stringObj, ScriptRuntime.emptyArgs);
            if (iterator instanceof Scriptable) {
                return new IteratorWrapper((Scriptable) iterator, scope);
            }
        }
        return Undefined.instance;
    }

    /** Checks if an object inherits from Iterator.prototype. */
    private static boolean isIteratorPrototypeInstance(Scriptable obj, Scriptable scope) {
        Scriptable iteratorPrototype = getIteratorPrototype(scope);
        if (iteratorPrototype == null) {
            return false;
        }

        Scriptable proto = obj.getPrototype();
        while (proto != null) {
            if (proto == iteratorPrototype) {
                return true;
            }
            proto = proto.getPrototype();
        }
        return false;
    }
}
