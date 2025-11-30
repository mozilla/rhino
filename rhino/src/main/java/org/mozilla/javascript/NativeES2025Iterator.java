/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/** ES2025 Iterator constructor and helper methods. */
public class NativeES2025Iterator extends ScriptableObject {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = "Iterator";

    static void init(Context cx, ScriptableObject scope, boolean sealed) {
        LambdaConstructor constructor =
                new LambdaConstructor(
                        scope,
                        CLASS_NAME,
                        0,
                        LambdaConstructor.CONSTRUCTOR_FUNCTION,
                        NativeES2025Iterator::constructor);

        constructor.defineConstructorMethod(scope, "from", 1, NativeES2025Iterator::js_from);
        constructor.defineConstructorMethod(scope, "concat", 0, NativeES2025Iterator::js_concat);

        constructor.definePrototypeMethod(
                scope,
                SymbolKey.ITERATOR,
                0,
                NativeES2025Iterator::js_iterator,
                DONTENUM,
                DONTENUM);
        constructor.definePrototypeMethod(
                scope, "map", 1, NativeES2025Iterator::js_map, DONTENUM, DONTENUM);
        constructor.definePrototypeMethod(
                scope, "filter", 1, NativeES2025Iterator::js_filter, DONTENUM, DONTENUM);
        constructor.definePrototypeMethod(
                scope, "take", 1, NativeES2025Iterator::js_take, DONTENUM, DONTENUM);
        constructor.definePrototypeMethod(
                scope, "drop", 1, NativeES2025Iterator::js_drop, DONTENUM, DONTENUM);
        constructor.definePrototypeMethod(
                scope, "flatMap", 1, NativeES2025Iterator::js_flatMap, DONTENUM, DONTENUM);
        constructor.definePrototypeMethod(
                scope, "reduce", 1, NativeES2025Iterator::js_reduce, DONTENUM, DONTENUM);
        constructor.definePrototypeMethod(
                scope, "toArray", 0, NativeES2025Iterator::js_toArray, DONTENUM, DONTENUM);
        constructor.definePrototypeMethod(
                scope, "forEach", 1, NativeES2025Iterator::js_forEach, DONTENUM, DONTENUM);
        constructor.definePrototypeMethod(
                scope, "some", 1, NativeES2025Iterator::js_some, DONTENUM, DONTENUM);
        constructor.definePrototypeMethod(
                scope, "every", 1, NativeES2025Iterator::js_every, DONTENUM, DONTENUM);
        constructor.definePrototypeMethod(
                scope, "find", 1, NativeES2025Iterator::js_find, DONTENUM, DONTENUM);

        constructor.definePrototypeProperty(
                SymbolKey.TO_STRING_TAG, CLASS_NAME, DONTENUM | READONLY);

        if (sealed) {
            constructor.sealObject();
            ((ScriptableObject) constructor.getPrototypeProperty()).sealObject();
        }

        ScriptableObject.defineProperty(scope, CLASS_NAME, constructor, DONTENUM);
    }

    private NativeES2025Iterator() {}

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    private static Scriptable constructor(Context cx, Scriptable scope, Object[] args) {
        throw ScriptRuntime.typeError("Iterator is not a constructor");
    }

    private static Object js_iterator(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return thisObj;
    }

    private static Object js_from(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object item = args.length > 0 ? args[0] : Undefined.instance;

        if (item == null || item == Undefined.instance) {
            throw ScriptRuntime.typeError("Cannot convert undefined or null to iterator");
        }

        if (item instanceof ES2025IteratorPrototype) {
            return item;
        }

        if (item instanceof Scriptable) {
            Scriptable scriptable = (Scriptable) item;
            Object iteratorMethod = ScriptableObject.getProperty(scriptable, SymbolKey.ITERATOR);

            if (iteratorMethod != Scriptable.NOT_FOUND && iteratorMethod instanceof Callable) {
                Callable callable = (Callable) iteratorMethod;
                Object iterator = callable.call(cx, scope, scriptable, ScriptRuntime.emptyArgs);
                return new WrappedIterator(cx, scope, iterator);
            }
        }

        throw ScriptRuntime.typeError("Object is not iterable");
    }

    private static Object js_concat(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable[] iterables = new Scriptable[args.length];
        Callable[] iteratorMethods = new Callable[args.length];

        for (int i = 0; i < args.length; i++) {
            Object item = args[i];

            if (!(item instanceof Scriptable)) {
                throw ScriptRuntime.typeError("Iterator.concat requires iterable objects");
            }

            Scriptable itemObj = (Scriptable) item;
            Object iteratorMethod = ScriptableObject.getProperty(itemObj, SymbolKey.ITERATOR);

            if (!(iteratorMethod instanceof Callable)) {
                throw ScriptRuntime.typeError(
                        "Iterator.concat item at index " + i + " is not iterable");
            }

            iterables[i] = itemObj;
            iteratorMethods[i] = (Callable) iteratorMethod;
        }

        return new ConcatIterator(cx, scope, iterables, iteratorMethods);
    }

    private static Callable getNextMethod(Object thisObj) {
        if (thisObj == null || !(thisObj instanceof Scriptable)) {
            throw ScriptRuntime.typeError("Iterator method called on non-object");
        }
        Object next = ScriptableObject.getProperty((Scriptable) thisObj, "next");
        if (!(next instanceof Callable)) {
            throw ScriptRuntime.typeError("Iterator must have a next method");
        }
        return (Callable) next;
    }

    private static Scriptable callNext(
            Context cx, Scriptable scope, Scriptable thisObj, Callable nextMethod) {
        Object result = nextMethod.call(cx, scope, thisObj, ScriptRuntime.emptyArgs);
        if (!(result instanceof Scriptable)) {
            throw ScriptRuntime.typeError("Iterator result must be an object");
        }
        return (Scriptable) result;
    }

    private static boolean isDone(Scriptable result) {
        Object done = ScriptableObject.getProperty(result, "done");
        return ScriptRuntime.toBoolean(done);
    }

    private static Object getValue(Scriptable result) {
        return ScriptableObject.getProperty(result, "value");
    }

    private static Object js_toArray(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Callable nextMethod = getNextMethod(thisObj);
        Scriptable array = cx.newArray(scope, 0);
        int index = 0;

        while (true) {
            Scriptable result = callNext(cx, scope, thisObj, nextMethod);
            if (isDone(result)) {
                break;
            }
            array.put(index++, array, getValue(result));
        }

        return array;
    }

    // Iterator.prototype.forEach(fn)
    private static Object js_forEach(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object fn = args.length > 0 ? args[0] : Undefined.instance;
        if (!(fn instanceof Callable)) {
            throw ScriptRuntime.typeError("forEach requires a function argument");
        }
        Callable callback = (Callable) fn;

        // Get the iterator's next method
        Object next = ScriptableObject.getProperty(thisObj, "next");
        if (!(next instanceof Callable)) {
            throw ScriptRuntime.typeError("Iterator must have a next method");
        }
        Callable nextMethod = (Callable) next;

        // Iterate and call function for each value
        long counter = 0;
        while (true) {
            Object result = nextMethod.call(cx, scope, thisObj, ScriptRuntime.emptyArgs);
            if (!(result instanceof Scriptable)) {
                throw ScriptRuntime.typeError("Iterator result must be an object");
            }
            Scriptable resultObj = (Scriptable) result;

            Object done = ScriptableObject.getProperty(resultObj, "done");
            if (ScriptRuntime.toBoolean(done)) {
                break;
            }

            Object value = ScriptableObject.getProperty(resultObj, "value");
            callback.call(cx, scope, Undefined.SCRIPTABLE_UNDEFINED, new Object[] {value, counter});
            counter++;
        }

        return Undefined.instance;
    }

    // Iterator.prototype.some(predicate)
    private static Object js_some(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object predicate = args.length > 0 ? args[0] : Undefined.instance;
        if (!(predicate instanceof Callable)) {
            throw ScriptRuntime.typeError("some requires a function argument");
        }
        Callable predicateFn = (Callable) predicate;

        // Get the iterator's next method
        Object next = ScriptableObject.getProperty(thisObj, "next");
        if (!(next instanceof Callable)) {
            throw ScriptRuntime.typeError("Iterator must have a next method");
        }
        Callable nextMethod = (Callable) next;

        // Check iterator return method for cleanup
        Object returnMethod = ScriptableObject.getProperty(thisObj, "return");

        long counter = 0;
        while (true) {
            Object result = nextMethod.call(cx, scope, thisObj, ScriptRuntime.emptyArgs);
            if (!(result instanceof Scriptable)) {
                throw ScriptRuntime.typeError("Iterator result must be an object");
            }
            Scriptable resultObj = (Scriptable) result;

            Object done = ScriptableObject.getProperty(resultObj, "done");
            if (ScriptRuntime.toBoolean(done)) {
                return Boolean.FALSE;
            }

            Object value = ScriptableObject.getProperty(resultObj, "value");
            Object testResult =
                    predicateFn.call(
                            cx,
                            scope,
                            Undefined.SCRIPTABLE_UNDEFINED,
                            new Object[] {value, counter});

            if (ScriptRuntime.toBoolean(testResult)) {
                // Call return method to close iterator
                if (returnMethod instanceof Callable) {
                    ((Callable) returnMethod).call(cx, scope, thisObj, ScriptRuntime.emptyArgs);
                }
                return Boolean.TRUE;
            }
            counter++;
        }
    }

    // Iterator.prototype.every(predicate)
    private static Object js_every(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object predicate = args.length > 0 ? args[0] : Undefined.instance;
        if (!(predicate instanceof Callable)) {
            throw ScriptRuntime.typeError("every requires a function argument");
        }
        Callable predicateFn = (Callable) predicate;

        // Get the iterator's next method
        Object next = ScriptableObject.getProperty(thisObj, "next");
        if (!(next instanceof Callable)) {
            throw ScriptRuntime.typeError("Iterator must have a next method");
        }
        Callable nextMethod = (Callable) next;

        // Check iterator return method for cleanup
        Object returnMethod = ScriptableObject.getProperty(thisObj, "return");

        long counter = 0;
        while (true) {
            Object result = nextMethod.call(cx, scope, thisObj, ScriptRuntime.emptyArgs);
            if (!(result instanceof Scriptable)) {
                throw ScriptRuntime.typeError("Iterator result must be an object");
            }
            Scriptable resultObj = (Scriptable) result;

            Object done = ScriptableObject.getProperty(resultObj, "done");
            if (ScriptRuntime.toBoolean(done)) {
                return Boolean.TRUE;
            }

            Object value = ScriptableObject.getProperty(resultObj, "value");
            Object testResult =
                    predicateFn.call(
                            cx,
                            scope,
                            Undefined.SCRIPTABLE_UNDEFINED,
                            new Object[] {value, counter});

            if (!ScriptRuntime.toBoolean(testResult)) {
                // Call return method to close iterator
                if (returnMethod instanceof Callable) {
                    ((Callable) returnMethod).call(cx, scope, thisObj, ScriptRuntime.emptyArgs);
                }
                return Boolean.FALSE;
            }
            counter++;
        }
    }

    // Iterator.prototype.find(predicate)
    private static Object js_find(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object predicate = args.length > 0 ? args[0] : Undefined.instance;
        if (!(predicate instanceof Callable)) {
            throw ScriptRuntime.typeError("find requires a function argument");
        }
        Callable predicateFn = (Callable) predicate;

        // Get the iterator's next method
        Object next = ScriptableObject.getProperty(thisObj, "next");
        if (!(next instanceof Callable)) {
            throw ScriptRuntime.typeError("Iterator must have a next method");
        }
        Callable nextMethod = (Callable) next;

        // Check iterator return method for cleanup
        Object returnMethod = ScriptableObject.getProperty(thisObj, "return");

        long counter = 0;
        while (true) {
            Object result = nextMethod.call(cx, scope, thisObj, ScriptRuntime.emptyArgs);
            if (!(result instanceof Scriptable)) {
                throw ScriptRuntime.typeError("Iterator result must be an object");
            }
            Scriptable resultObj = (Scriptable) result;

            Object done = ScriptableObject.getProperty(resultObj, "done");
            if (ScriptRuntime.toBoolean(done)) {
                return Undefined.instance;
            }

            Object value = ScriptableObject.getProperty(resultObj, "value");
            Object testResult =
                    predicateFn.call(
                            cx,
                            scope,
                            Undefined.SCRIPTABLE_UNDEFINED,
                            new Object[] {value, counter});

            if (ScriptRuntime.toBoolean(testResult)) {
                // Call return method to close iterator
                if (returnMethod instanceof Callable) {
                    ((Callable) returnMethod).call(cx, scope, thisObj, ScriptRuntime.emptyArgs);
                }
                return value;
            }
            counter++;
        }
    }

    // Iterator.prototype.reduce(reducer, initialValue)
    private static Object js_reduce(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object reducer = args.length > 0 ? args[0] : Undefined.instance;
        if (!(reducer instanceof Callable)) {
            throw ScriptRuntime.typeError("reduce requires a function argument");
        }
        Callable reducerFn = (Callable) reducer;

        // Get the iterator's next method
        Object next = ScriptableObject.getProperty(thisObj, "next");
        if (!(next instanceof Callable)) {
            throw ScriptRuntime.typeError("Iterator must have a next method");
        }
        Callable nextMethod = (Callable) next;

        Object accumulator;
        long counter;

        // Check if initial value was provided
        if (args.length >= 2) {
            accumulator = args[1];
            counter = 0;
        } else {
            // No initial value - use first value from iterator
            Object result = nextMethod.call(cx, scope, thisObj, ScriptRuntime.emptyArgs);
            if (!(result instanceof Scriptable)) {
                throw ScriptRuntime.typeError("Iterator result must be an object");
            }
            Scriptable resultObj = (Scriptable) result;

            Object done = ScriptableObject.getProperty(resultObj, "done");
            if (ScriptRuntime.toBoolean(done)) {
                throw ScriptRuntime.typeError("Reduce of empty iterator with no initial value");
            }

            accumulator = ScriptableObject.getProperty(resultObj, "value");
            counter = 1;
        }

        // Iterate and reduce
        while (true) {
            Object result = nextMethod.call(cx, scope, thisObj, ScriptRuntime.emptyArgs);
            if (!(result instanceof Scriptable)) {
                throw ScriptRuntime.typeError("Iterator result must be an object");
            }
            Scriptable resultObj = (Scriptable) result;

            Object done = ScriptableObject.getProperty(resultObj, "done");
            if (ScriptRuntime.toBoolean(done)) {
                break;
            }

            Object value = ScriptableObject.getProperty(resultObj, "value");
            accumulator =
                    reducerFn.call(
                            cx,
                            scope,
                            Undefined.SCRIPTABLE_UNDEFINED,
                            new Object[] {accumulator, value, counter});
            counter++;
        }

        return accumulator;
    }

    // Iterator.prototype.map(mapper)
    private static Object js_map(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object mapper = args.length > 0 ? args[0] : Undefined.instance;
        if (!(mapper instanceof Callable)) {
            throw ScriptRuntime.typeError("map requires a function argument");
        }
        Callable mapperFn = (Callable) mapper;

        // Get the iterator's next method
        Object next = ScriptableObject.getProperty(thisObj, "next");
        if (!(next instanceof Callable)) {
            throw ScriptRuntime.typeError("Iterator must have a next method");
        }

        return new MapIterator(cx, scope, thisObj, (Callable) next, mapperFn);
    }

    // Iterator.prototype.filter(predicate)
    private static Object js_filter(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object predicate = args.length > 0 ? args[0] : Undefined.instance;
        if (!(predicate instanceof Callable)) {
            throw ScriptRuntime.typeError("filter requires a function argument");
        }
        Callable predicateFn = (Callable) predicate;

        // Get the iterator's next method
        Object next = ScriptableObject.getProperty(thisObj, "next");
        if (!(next instanceof Callable)) {
            throw ScriptRuntime.typeError("Iterator must have a next method");
        }

        return new FilterIterator(cx, scope, thisObj, (Callable) next, predicateFn);
    }

    // Iterator.prototype.take(limit)
    private static Object js_take(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double limit = args.length > 0 ? ScriptRuntime.toNumber(args[0]) : Double.NaN;

        if (Double.isNaN(limit)) {
            throw ScriptRuntime.rangeError("take limit must be a number");
        }
        if (limit < 0) {
            throw ScriptRuntime.rangeError("take limit must be non-negative");
        }

        long remaining = (long) Math.floor(limit);

        // Get the iterator's next method
        Object next = ScriptableObject.getProperty(thisObj, "next");
        if (!(next instanceof Callable)) {
            throw ScriptRuntime.typeError("Iterator must have a next method");
        }

        return new TakeIterator(cx, scope, thisObj, (Callable) next, remaining);
    }

    // Iterator.prototype.drop(limit)
    private static Object js_drop(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double limit = args.length > 0 ? ScriptRuntime.toNumber(args[0]) : Double.NaN;

        if (Double.isNaN(limit)) {
            throw ScriptRuntime.rangeError("drop limit must be a number");
        }
        if (limit < 0) {
            throw ScriptRuntime.rangeError("drop limit must be non-negative");
        }

        long toSkip = (long) Math.floor(limit);

        // Get the iterator's next method
        Object next = ScriptableObject.getProperty(thisObj, "next");
        if (!(next instanceof Callable)) {
            throw ScriptRuntime.typeError("Iterator must have a next method");
        }

        return new DropIterator(cx, scope, thisObj, (Callable) next, toSkip);
    }

    // Iterator.prototype.flatMap(mapper)
    private static Object js_flatMap(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Object mapper = args.length > 0 ? args[0] : Undefined.instance;
        if (!(mapper instanceof Callable)) {
            throw ScriptRuntime.typeError("flatMap requires a function argument");
        }
        Callable mapperFn = (Callable) mapper;

        // Get the iterator's next method
        Object next = ScriptableObject.getProperty(thisObj, "next");
        if (!(next instanceof Callable)) {
            throw ScriptRuntime.typeError("Iterator must have a next method");
        }

        return new FlatMapIterator(cx, scope, thisObj, (Callable) next, mapperFn);
    }

    /** Base class for iterators that inherit from Iterator.prototype */
    abstract static class ES2025IteratorPrototype extends ScriptableObject {

        ES2025IteratorPrototype() {
            // Define next() method that calls the abstract next() implementation
            defineProperty(
                    "next",
                    new BaseFunction() {
                        @Override
                        public Object call(
                                Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                            if (!(thisObj instanceof ES2025IteratorPrototype)) {
                                throw ScriptRuntime.typeError("next called on incompatible object");
                            }
                            return ((ES2025IteratorPrototype) thisObj).next(cx, scope);
                        }

                        @Override
                        public int getLength() {
                            return 0;
                        }

                        @Override
                        public String getFunctionName() {
                            return "next";
                        }
                    },
                    DONTENUM);
        }

        abstract Object next(Context cx, Scriptable scope);

        Object doReturn(Context cx, Scriptable scope, Object value) {
            // Default implementation - just return done: true, value
            Scriptable result = cx.newObject(scope);
            ScriptableObject.putProperty(result, "done", Boolean.TRUE);
            ScriptableObject.putProperty(result, "value", value);
            return result;
        }

        Object doThrow(Context cx, Scriptable scope, Object value) {
            // Default implementation - throw the value
            if (value instanceof JavaScriptException) {
                throw (JavaScriptException) value;
            } else if (value instanceof RhinoException) {
                throw (RhinoException) value;
            }
            throw ScriptRuntime.typeError(value.toString());
        }

        @Override
        public String getClassName() {
            return "Iterator";
        }
    }

    /**
     * Wrapper for iterators returned by Iterator.from() to ensure they inherit from
     * Iterator.prototype
     */
    private static class WrappedIterator extends ES2025IteratorPrototype {
        private final Object wrappedIterator;
        private final Callable nextMethod;
        private Callable returnMethod;
        private Callable throwMethod;

        WrappedIterator(Context cx, Scriptable scope, Object iterator) {
            this.wrappedIterator = iterator;

            // Get the next method
            if (iterator instanceof Scriptable) {
                Scriptable iterScriptable = (Scriptable) iterator;
                Object next = ScriptableObject.getProperty(iterScriptable, "next");
                if (next instanceof Callable) {
                    this.nextMethod = (Callable) next;
                } else {
                    throw ScriptRuntime.typeError("Iterator missing next method");
                }

                // Get optional return method
                Object ret = ScriptableObject.getProperty(iterScriptable, "return");
                if (ret instanceof Callable) {
                    this.returnMethod = (Callable) ret;
                }

                // Get optional throw method
                Object thr = ScriptableObject.getProperty(iterScriptable, "throw");
                if (thr instanceof Callable) {
                    this.throwMethod = (Callable) thr;
                }

                // Set up prototype chain to inherit from Iterator.prototype
                Scriptable topScope = ScriptableObject.getTopLevelScope(scope);
                Scriptable iteratorProto = ScriptableObject.getClassPrototype(topScope, "Iterator");
                if (iteratorProto != null) {
                    this.setPrototype(iteratorProto);
                }
                this.setParentScope(scope);
            } else {
                throw ScriptRuntime.typeError("Iterator must be an object");
            }
        }

        @Override
        Object next(Context cx, Scriptable scope) {
            if (wrappedIterator instanceof Scriptable) {
                return nextMethod.call(
                        cx, scope, (Scriptable) wrappedIterator, ScriptRuntime.emptyArgs);
            }
            throw ScriptRuntime.typeError("Invalid iterator");
        }

        @Override
        Object doReturn(Context cx, Scriptable scope, Object value) {
            if (returnMethod != null && wrappedIterator instanceof Scriptable) {
                return returnMethod.call(
                        cx, scope, (Scriptable) wrappedIterator, new Object[] {value});
            }
            return super.doReturn(cx, scope, value);
        }

        @Override
        Object doThrow(Context cx, Scriptable scope, Object value) {
            if (throwMethod != null && wrappedIterator instanceof Scriptable) {
                return throwMethod.call(
                        cx, scope, (Scriptable) wrappedIterator, new Object[] {value});
            }
            return super.doThrow(cx, scope, value);
        }
    }

    /** Iterator returned by map() */
    private static class MapIterator extends ES2025IteratorPrototype {
        private final Scriptable sourceIterator;
        private final Callable nextMethod;
        private final Callable mapper;
        private long counter = 0;

        MapIterator(
                Context cx,
                Scriptable scope,
                Scriptable sourceIterator,
                Callable nextMethod,
                Callable mapper) {
            this.sourceIterator = sourceIterator;
            this.nextMethod = nextMethod;
            this.mapper = mapper;

            // Set up prototype chain
            Scriptable topScope = ScriptableObject.getTopLevelScope(scope);
            Scriptable iteratorProto = ScriptableObject.getClassPrototype(topScope, "Iterator");
            if (iteratorProto != null) {
                this.setPrototype(iteratorProto);
            }
            this.setParentScope(scope);
        }

        @Override
        Object next(Context cx, Scriptable scope) {
            // Get next value from source iterator
            Object result = nextMethod.call(cx, scope, sourceIterator, ScriptRuntime.emptyArgs);
            if (!(result instanceof Scriptable)) {
                throw ScriptRuntime.typeError("Iterator result must be an object");
            }
            Scriptable resultObj = (Scriptable) result;

            Object done = ScriptableObject.getProperty(resultObj, "done");
            if (ScriptRuntime.toBoolean(done)) {
                return result;
            }

            // Map the value
            Object value = ScriptableObject.getProperty(resultObj, "value");
            Object mappedValue =
                    mapper.call(
                            cx,
                            scope,
                            Undefined.SCRIPTABLE_UNDEFINED,
                            new Object[] {value, counter});
            counter++;

            // Return new result with mapped value
            Scriptable newResult = cx.newObject(scope);
            ScriptableObject.putProperty(newResult, "done", Boolean.FALSE);
            ScriptableObject.putProperty(newResult, "value", mappedValue);
            return newResult;
        }
    }

    /** Iterator returned by filter() */
    private static class FilterIterator extends ES2025IteratorPrototype {
        private final Scriptable sourceIterator;
        private final Callable nextMethod;
        private final Callable predicate;
        private long counter = 0;

        FilterIterator(
                Context cx,
                Scriptable scope,
                Scriptable sourceIterator,
                Callable nextMethod,
                Callable predicate) {
            this.sourceIterator = sourceIterator;
            this.nextMethod = nextMethod;
            this.predicate = predicate;

            // Set up prototype chain
            Scriptable topScope = ScriptableObject.getTopLevelScope(scope);
            Scriptable iteratorProto = ScriptableObject.getClassPrototype(topScope, "Iterator");
            if (iteratorProto != null) {
                this.setPrototype(iteratorProto);
            }
            this.setParentScope(scope);
        }

        @Override
        Object next(Context cx, Scriptable scope) {
            while (true) {
                Object result = nextMethod.call(cx, scope, sourceIterator, ScriptRuntime.emptyArgs);
                if (!(result instanceof Scriptable)) {
                    throw ScriptRuntime.typeError("Iterator result must be an object");
                }
                Scriptable resultObj = (Scriptable) result;

                Object done = ScriptableObject.getProperty(resultObj, "done");
                if (ScriptRuntime.toBoolean(done)) {
                    return result;
                }

                Object value = ScriptableObject.getProperty(resultObj, "value");
                Object testResult =
                        predicate.call(
                                cx,
                                scope,
                                Undefined.SCRIPTABLE_UNDEFINED,
                                new Object[] {value, counter});
                counter++;

                if (ScriptRuntime.toBoolean(testResult)) {
                    return result;
                }
            }
        }
    }

    /** Iterator returned by take() */
    private static class TakeIterator extends ES2025IteratorPrototype {
        private final Scriptable sourceIterator;
        private final Callable nextMethod;
        private long remaining;

        TakeIterator(
                Context cx,
                Scriptable scope,
                Scriptable sourceIterator,
                Callable nextMethod,
                long remaining) {
            this.sourceIterator = sourceIterator;
            this.nextMethod = nextMethod;
            this.remaining = remaining;

            // Set up prototype chain
            Scriptable topScope = ScriptableObject.getTopLevelScope(scope);
            Scriptable iteratorProto = ScriptableObject.getClassPrototype(topScope, "Iterator");
            if (iteratorProto != null) {
                this.setPrototype(iteratorProto);
            }
            this.setParentScope(scope);
        }

        @Override
        Object next(Context cx, Scriptable scope) {
            if (remaining <= 0) {
                // Close the underlying iterator
                Object returnMethod = ScriptableObject.getProperty(sourceIterator, "return");
                if (returnMethod instanceof Callable) {
                    ((Callable) returnMethod)
                            .call(cx, scope, sourceIterator, ScriptRuntime.emptyArgs);
                }

                // Return done result
                Scriptable result = cx.newObject(scope);
                ScriptableObject.putProperty(result, "done", Boolean.TRUE);
                ScriptableObject.putProperty(result, "value", Undefined.instance);
                return result;
            }

            Object result = nextMethod.call(cx, scope, sourceIterator, ScriptRuntime.emptyArgs);
            remaining--;
            return result;
        }
    }

    /** Iterator returned by drop() */
    private static class DropIterator extends ES2025IteratorPrototype {
        private final Scriptable sourceIterator;
        private final Callable nextMethod;
        private long toSkip;

        DropIterator(
                Context cx,
                Scriptable scope,
                Scriptable sourceIterator,
                Callable nextMethod,
                long toSkip) {
            this.sourceIterator = sourceIterator;
            this.nextMethod = nextMethod;
            this.toSkip = toSkip;

            // Set up prototype chain
            Scriptable topScope = ScriptableObject.getTopLevelScope(scope);
            Scriptable iteratorProto = ScriptableObject.getClassPrototype(topScope, "Iterator");
            if (iteratorProto != null) {
                this.setPrototype(iteratorProto);
            }
            this.setParentScope(scope);
        }

        @Override
        Object next(Context cx, Scriptable scope) {
            // Skip the required number of items
            while (toSkip > 0) {
                Object result = nextMethod.call(cx, scope, sourceIterator, ScriptRuntime.emptyArgs);
                if (!(result instanceof Scriptable)) {
                    throw ScriptRuntime.typeError("Iterator result must be an object");
                }
                Scriptable resultObj = (Scriptable) result;

                Object done = ScriptableObject.getProperty(resultObj, "done");
                if (ScriptRuntime.toBoolean(done)) {
                    return result;
                }
                toSkip--;
            }

            // Return next value from source
            return nextMethod.call(cx, scope, sourceIterator, ScriptRuntime.emptyArgs);
        }
    }

    /** Iterator returned by flatMap() */
    private static class FlatMapIterator extends ES2025IteratorPrototype {
        private final Scriptable sourceIterator;
        private final Callable nextMethod;
        private final Callable mapper;
        private long counter = 0;
        private Scriptable innerIterator = null;
        private Callable innerNextMethod = null;

        FlatMapIterator(
                Context cx,
                Scriptable scope,
                Scriptable sourceIterator,
                Callable nextMethod,
                Callable mapper) {
            this.sourceIterator = sourceIterator;
            this.nextMethod = nextMethod;
            this.mapper = mapper;

            // Set up prototype chain
            Scriptable topScope = ScriptableObject.getTopLevelScope(scope);
            Scriptable iteratorProto = ScriptableObject.getClassPrototype(topScope, "Iterator");
            if (iteratorProto != null) {
                this.setPrototype(iteratorProto);
            }
            this.setParentScope(scope);
        }

        @Override
        Object next(Context cx, Scriptable scope) {
            while (true) {
                // If we have an inner iterator, try to get next value from it
                if (innerIterator != null) {
                    Object innerResult =
                            innerNextMethod.call(cx, scope, innerIterator, ScriptRuntime.emptyArgs);
                    if (!(innerResult instanceof Scriptable)) {
                        throw ScriptRuntime.typeError("Iterator result must be an object");
                    }
                    Scriptable innerResultObj = (Scriptable) innerResult;

                    Object innerDone = ScriptableObject.getProperty(innerResultObj, "done");
                    if (!ScriptRuntime.toBoolean(innerDone)) {
                        return innerResult;
                    }

                    // Inner iterator exhausted, move to next source value
                    innerIterator = null;
                    innerNextMethod = null;
                }

                // Get next value from source iterator
                Object result = nextMethod.call(cx, scope, sourceIterator, ScriptRuntime.emptyArgs);
                if (!(result instanceof Scriptable)) {
                    throw ScriptRuntime.typeError("Iterator result must be an object");
                }
                Scriptable resultObj = (Scriptable) result;

                Object done = ScriptableObject.getProperty(resultObj, "done");
                if (ScriptRuntime.toBoolean(done)) {
                    return result;
                }

                // Map the value to an iterator
                Object value = ScriptableObject.getProperty(resultObj, "value");
                Object mapped =
                        mapper.call(
                                cx,
                                scope,
                                Undefined.SCRIPTABLE_UNDEFINED,
                                new Object[] {value, counter});
                counter++;

                // Reject strings (they're iterable but shouldn't be flattened)
                if (mapped instanceof String || mapped instanceof ConsString) {
                    throw ScriptRuntime.typeError("flatMap mapper cannot return a string");
                }

                // Get the iterator from the mapped value
                if (mapped instanceof Scriptable) {
                    Scriptable mappedObj = (Scriptable) mapped;
                    Object iteratorMethod =
                            ScriptableObject.getProperty(mappedObj, SymbolKey.ITERATOR);

                    if (iteratorMethod instanceof Callable) {
                        Object iter =
                                ((Callable) iteratorMethod)
                                        .call(cx, scope, mappedObj, ScriptRuntime.emptyArgs);
                        if (iter instanceof Scriptable) {
                            innerIterator = (Scriptable) iter;
                            Object next = ScriptableObject.getProperty(innerIterator, "next");
                            if (next instanceof Callable) {
                                innerNextMethod = (Callable) next;
                                continue;
                            }
                        }
                    }
                }

                throw ScriptRuntime.typeError("flatMap mapper must return an iterable");
            }
        }
    }

    /** Iterator returned by concat() */
    private static class ConcatIterator extends ES2025IteratorPrototype {
        private final Scriptable[] iterables;
        private final Callable[] iteratorMethods;
        private int currentIndex = 0;
        private Scriptable currentIterator = null;
        private Callable currentNextMethod = null;

        ConcatIterator(
                Context cx, Scriptable scope, Scriptable[] iterables, Callable[] iteratorMethods) {
            this.iterables = iterables;
            this.iteratorMethods = iteratorMethods;

            // Set up prototype chain
            Scriptable topScope = ScriptableObject.getTopLevelScope(scope);
            Scriptable iteratorProto = ScriptableObject.getClassPrototype(topScope, "Iterator");
            if (iteratorProto != null) {
                this.setPrototype(iteratorProto);
            }
            this.setParentScope(scope);
        }

        @Override
        Object next(Context cx, Scriptable scope) {
            while (true) {
                // If we don't have a current iterator, get the next one
                if (currentIterator == null) {
                    if (currentIndex >= iterables.length) {
                        // All iterables exhausted
                        Scriptable result = cx.newObject(scope);
                        ScriptableObject.putProperty(result, "done", Boolean.TRUE);
                        ScriptableObject.putProperty(result, "value", Undefined.instance);
                        return result;
                    }

                    // Get iterator for current iterable
                    Scriptable iterable = iterables[currentIndex];
                    Callable iteratorMethod = iteratorMethods[currentIndex];

                    Object iterator =
                            iteratorMethod.call(cx, scope, iterable, ScriptRuntime.emptyArgs);
                    if (!(iterator instanceof Scriptable)) {
                        throw ScriptRuntime.typeError("Iterator method must return an object");
                    }

                    currentIterator = (Scriptable) iterator;
                    Object next = ScriptableObject.getProperty(currentIterator, "next");
                    if (!(next instanceof Callable)) {
                        throw ScriptRuntime.typeError("Iterator must have a next method");
                    }
                    currentNextMethod = (Callable) next;
                }

                // Get next value from current iterator
                Object result =
                        currentNextMethod.call(cx, scope, currentIterator, ScriptRuntime.emptyArgs);
                if (!(result instanceof Scriptable)) {
                    throw ScriptRuntime.typeError("Iterator result must be an object");
                }
                Scriptable resultObj = (Scriptable) result;

                Object done = ScriptableObject.getProperty(resultObj, "done");
                if (ScriptRuntime.toBoolean(done)) {
                    // Current iterator exhausted, move to next
                    currentIterator = null;
                    currentNextMethod = null;
                    currentIndex++;
                    continue;
                }

                return result;
            }
        }

        @Override
        Object doReturn(Context cx, Scriptable scope, Object value) {
            // Close current iterator if active
            if (currentIterator != null) {
                Object returnMethod = ScriptableObject.getProperty(currentIterator, "return");
                if (returnMethod instanceof Callable) {
                    ((Callable) returnMethod)
                            .call(cx, scope, currentIterator, new Object[] {value});
                }
            }
            return super.doReturn(cx, scope, value);
        }
    }
}
