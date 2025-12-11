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

    /** Validates that thisObj is a Scriptable, throws TypeError if not */
    private static Scriptable requireObjectCoercible(Object thisObj, String methodName) {
        if (thisObj == null || !(thisObj instanceof Scriptable)) {
            throw ScriptRuntime.typeError(
                    "Iterator.prototype." + methodName + " called on non-object");
        }
        return (Scriptable) thisObj;
    }

    private static Callable getNextMethod(Scriptable thisObj) {
        Object next = ScriptableObject.getProperty(thisObj, "next");
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
        Scriptable iterator = requireObjectCoercible(thisObj, "toArray");
        Callable nextMethod = getNextMethod(iterator);
        Scriptable array = cx.newArray(scope, 0);
        int index = 0;

        while (true) {
            Scriptable result = callNext(cx, scope, iterator, nextMethod);
            if (isDone(result)) {
                break;
            }
            array.put(index++, array, getValue(result));
        }

        return array;
    }

    private static Object js_forEach(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable iterator = requireObjectCoercible(thisObj, "forEach");

        Object fn = args.length > 0 ? args[0] : Undefined.instance;
        if (!(fn instanceof Callable)) {
            throw ScriptRuntime.typeError("forEach requires a function argument");
        }
        Callable callback = (Callable) fn;

        Callable nextMethod = getNextMethod(iterator);

        long counter = 0;
        while (true) {
            Object result = nextMethod.call(cx, scope, iterator, ScriptRuntime.emptyArgs);
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

    private static Object js_some(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable iterator = requireObjectCoercible(thisObj, "some");

        Object predicate = args.length > 0 ? args[0] : Undefined.instance;
        if (!(predicate instanceof Callable)) {
            throw ScriptRuntime.typeError("some requires a function argument");
        }
        Callable predicateFn = (Callable) predicate;

        Callable nextMethod = getNextMethod(iterator);
        Object returnMethod = ScriptableObject.getProperty(iterator, "return");

        long counter = 0;
        while (true) {
            Object result = nextMethod.call(cx, scope, iterator, ScriptRuntime.emptyArgs);
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
                if (returnMethod instanceof Callable) {
                    ((Callable) returnMethod).call(cx, scope, iterator, ScriptRuntime.emptyArgs);
                }
                return Boolean.TRUE;
            }
            counter++;
        }
    }

    private static Object js_every(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable iterator = requireObjectCoercible(thisObj, "every");

        Object predicate = args.length > 0 ? args[0] : Undefined.instance;
        if (!(predicate instanceof Callable)) {
            throw ScriptRuntime.typeError("every requires a function argument");
        }
        Callable predicateFn = (Callable) predicate;

        Callable nextMethod = getNextMethod(iterator);
        Object returnMethod = ScriptableObject.getProperty(iterator, "return");

        long counter = 0;
        while (true) {
            Object result = nextMethod.call(cx, scope, iterator, ScriptRuntime.emptyArgs);
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
                if (returnMethod instanceof Callable) {
                    ((Callable) returnMethod).call(cx, scope, iterator, ScriptRuntime.emptyArgs);
                }
                return Boolean.FALSE;
            }
            counter++;
        }
    }

    private static Object js_find(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable iterator = requireObjectCoercible(thisObj, "find");

        Object predicate = args.length > 0 ? args[0] : Undefined.instance;
        if (!(predicate instanceof Callable)) {
            throw ScriptRuntime.typeError("find requires a function argument");
        }
        Callable predicateFn = (Callable) predicate;

        Callable nextMethod = getNextMethod(iterator);
        Object returnMethod = ScriptableObject.getProperty(iterator, "return");

        long counter = 0;
        while (true) {
            Object result = nextMethod.call(cx, scope, iterator, ScriptRuntime.emptyArgs);
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
                if (returnMethod instanceof Callable) {
                    ((Callable) returnMethod).call(cx, scope, iterator, ScriptRuntime.emptyArgs);
                }
                return value;
            }
            counter++;
        }
    }

    private static Object js_reduce(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable iterator = requireObjectCoercible(thisObj, "reduce");

        Object reducer = args.length > 0 ? args[0] : Undefined.instance;
        if (!(reducer instanceof Callable)) {
            throw ScriptRuntime.typeError("reduce requires a function argument");
        }
        Callable reducerFn = (Callable) reducer;

        Callable nextMethod = getNextMethod(iterator);

        Object accumulator;
        long counter;

        if (args.length >= 2) {
            accumulator = args[1];
            counter = 0;
        } else {
            Object result = nextMethod.call(cx, scope, iterator, ScriptRuntime.emptyArgs);
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

        while (true) {
            Object result = nextMethod.call(cx, scope, iterator, ScriptRuntime.emptyArgs);
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

    private static Object js_map(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable iterator = requireObjectCoercible(thisObj, "map");

        Object mapper = args.length > 0 ? args[0] : Undefined.instance;
        if (!(mapper instanceof Callable)) {
            throw ScriptRuntime.typeError("map requires a function argument");
        }
        Callable mapperFn = (Callable) mapper;

        Callable nextMethod = getNextMethod(iterator);
        return new MapIterator(cx, scope, iterator, nextMethod, mapperFn);
    }

    private static Object js_filter(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable iterator = requireObjectCoercible(thisObj, "filter");

        Object predicate = args.length > 0 ? args[0] : Undefined.instance;
        if (!(predicate instanceof Callable)) {
            throw ScriptRuntime.typeError("filter requires a function argument");
        }
        Callable predicateFn = (Callable) predicate;

        Callable nextMethod = getNextMethod(iterator);
        return new FilterIterator(cx, scope, iterator, nextMethod, predicateFn);
    }

    private static Object js_take(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable iterator = requireObjectCoercible(thisObj, "take");

        double limit = args.length > 0 ? ScriptRuntime.toNumber(args[0]) : Double.NaN;

        if (Double.isNaN(limit)) {
            throw ScriptRuntime.rangeError("take limit must be a number");
        }
        if (limit < 0) {
            throw ScriptRuntime.rangeError("take limit must be non-negative");
        }

        long remaining = (long) Math.floor(limit);
        Callable nextMethod = getNextMethod(iterator);
        return new TakeIterator(cx, scope, iterator, nextMethod, remaining);
    }

    private static Object js_drop(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable iterator = requireObjectCoercible(thisObj, "drop");

        double limit = args.length > 0 ? ScriptRuntime.toNumber(args[0]) : Double.NaN;

        if (Double.isNaN(limit)) {
            throw ScriptRuntime.rangeError("drop limit must be a number");
        }
        if (limit < 0) {
            throw ScriptRuntime.rangeError("drop limit must be non-negative");
        }

        long toSkip = (long) Math.floor(limit);
        Callable nextMethod = getNextMethod(iterator);
        return new DropIterator(cx, scope, iterator, nextMethod, toSkip);
    }

    private static Object js_flatMap(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable iterator = requireObjectCoercible(thisObj, "flatMap");

        Object mapper = args.length > 0 ? args[0] : Undefined.instance;
        if (!(mapper instanceof Callable)) {
            throw ScriptRuntime.typeError("flatMap requires a function argument");
        }
        Callable mapperFn = (Callable) mapper;

        Callable nextMethod = getNextMethod(iterator);
        return new FlatMapIterator(cx, scope, iterator, nextMethod, mapperFn);
    }

    /** Base class for iterators that inherit from Iterator.prototype */
    abstract static class ES2025IteratorPrototype extends ScriptableObject {
        private boolean isExecuting = false;

        ES2025IteratorPrototype() {
            defineProperty(
                    "next",
                    new BaseFunction() {
                        @Override
                        public Object call(
                                Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                            if (!(thisObj instanceof ES2025IteratorPrototype)) {
                                throw ScriptRuntime.typeError("next called on incompatible object");
                            }
                            ES2025IteratorPrototype self = (ES2025IteratorPrototype) thisObj;
                            if (self.isExecuting) {
                                throw ScriptRuntime.typeError(
                                        "Iterator Helper generator is already running");
                            }
                            self.isExecuting = true;
                            try {
                                return self.next(cx, scope);
                            } finally {
                                self.isExecuting = false;
                            }
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
            Scriptable result = cx.newObject(scope);
            ScriptableObject.putProperty(result, "done", Boolean.TRUE);
            ScriptableObject.putProperty(result, "value", value);
            return result;
        }

        Object doThrow(Context cx, Scriptable scope, Object value) {
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

            Scriptable topScope = ScriptableObject.getTopLevelScope(scope);
            Scriptable iteratorProto = ScriptableObject.getClassPrototype(topScope, "Iterator");
            if (iteratorProto != null) {
                this.setPrototype(iteratorProto);
            }
            this.setParentScope(scope);
        }

        @Override
        Object next(Context cx, Scriptable scope) {
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
            Object mappedValue =
                    mapper.call(
                            cx,
                            scope,
                            Undefined.SCRIPTABLE_UNDEFINED,
                            new Object[] {value, counter});
            counter++;

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
                Object returnMethod = ScriptableObject.getProperty(sourceIterator, "return");
                if (returnMethod instanceof Callable) {
                    ((Callable) returnMethod)
                            .call(cx, scope, sourceIterator, ScriptRuntime.emptyArgs);
                }

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

            Scriptable topScope = ScriptableObject.getTopLevelScope(scope);
            Scriptable iteratorProto = ScriptableObject.getClassPrototype(topScope, "Iterator");
            if (iteratorProto != null) {
                this.setPrototype(iteratorProto);
            }
            this.setParentScope(scope);
        }

        @Override
        Object next(Context cx, Scriptable scope) {
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

                    innerIterator = null;
                    innerNextMethod = null;
                }

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
                Object mapped =
                        mapper.call(
                                cx,
                                scope,
                                Undefined.SCRIPTABLE_UNDEFINED,
                                new Object[] {value, counter});
                counter++;

                if (mapped instanceof String || mapped instanceof ConsString) {
                    throw ScriptRuntime.typeError("flatMap mapper cannot return a string");
                }

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
                if (currentIterator == null) {
                    if (currentIndex >= iterables.length) {
                        Scriptable result = cx.newObject(scope);
                        ScriptableObject.putProperty(result, "done", Boolean.TRUE);
                        ScriptableObject.putProperty(result, "value", Undefined.instance);
                        return result;
                    }

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

                Object result =
                        currentNextMethod.call(cx, scope, currentIterator, ScriptRuntime.emptyArgs);
                if (!(result instanceof Scriptable)) {
                    throw ScriptRuntime.typeError("Iterator result must be an object");
                }
                Scriptable resultObj = (Scriptable) result;

                Object done = ScriptableObject.getProperty(resultObj, "done");
                if (ScriptRuntime.toBoolean(done)) {
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
