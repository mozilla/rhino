/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * The ES2025 global "Iterator" constructor, %Iterator.prototype% including the iterator helper
 * methods, %IteratorHelperPrototype% and %WrapForValidIteratorPrototype%.
 *
 * <p>For language versions below {@link Context#VERSION_ECMASCRIPT} the legacy {@link
 * NativeIterator} stays the global "Iterator", and only a hidden %Iterator.prototype% is created so
 * that the built-in iterators share a common prototype.
 */
public final class NativeES2025Iterator extends ScriptableObject {
    private static final long serialVersionUID = 7614590447128843560L;

    private static final String CLASS_NAME = "Iterator";
    private static final String HELPER_TAG = "Iterator Helper";

    static final Object ITERATOR_PROTOTYPE_TAG = "IteratorPrototype";
    private static final Object HELPER_PROTOTYPE_TAG = "IteratorHelperPrototype";
    private static final Object WRAP_PROTOTYPE_TAG = "WrapForValidIteratorPrototype";

    private static final Object DONE = new Object();

    static void init(Context cx, ScriptableObject scope, boolean sealed) {
        if (cx.getLanguageVersion() < Context.VERSION_ECMASCRIPT) {
            NativeObject proto = newPrototype(scope, getObjectPrototype(scope));
            defineMethod(scope, proto, SymbolKey.ITERATOR, 0, NativeES2025Iterator::js_iterator);
            finishPrototype(scope, proto, ITERATOR_PROTOTYPE_TAG, sealed);
            return;
        }

        LambdaConstructor ctor =
                new LambdaConstructor(
                        scope,
                        CLASS_NAME,
                        0,
                        NativeES2025Iterator::js_abstract,
                        NativeES2025Iterator::js_construct);
        ctor.setPrototypePropertyAttributes(PERMANENT | READONLY | DONTENUM);
        NativeObject proto = new NativeObject();
        ctor.setPrototypeScriptable(proto);

        ctor.defineConstructorMethod(scope, "from", 1, NativeES2025Iterator::js_from);
        ctor.defineConstructorMethod(scope, "concat", 0, NativeES2025Iterator::js_concat);
        ctor.definePrototypeMethod(scope, SymbolKey.ITERATOR, 0, NativeES2025Iterator::js_iterator);
        ctor.definePrototypeMethod(scope, "map", 1, NativeES2025Iterator::js_map);
        ctor.definePrototypeMethod(scope, "filter", 1, NativeES2025Iterator::js_filter);
        ctor.definePrototypeMethod(scope, "take", 1, NativeES2025Iterator::js_take);
        ctor.definePrototypeMethod(scope, "drop", 1, NativeES2025Iterator::js_drop);
        ctor.definePrototypeMethod(scope, "flatMap", 1, NativeES2025Iterator::js_flatMap);
        ctor.definePrototypeMethod(scope, "reduce", 1, NativeES2025Iterator::js_reduce);
        ctor.definePrototypeMethod(scope, "toArray", 0, NativeES2025Iterator::js_toArray);
        ctor.definePrototypeMethod(scope, "forEach", 1, NativeES2025Iterator::js_forEach);
        ctor.definePrototypeMethod(scope, "some", 1, NativeES2025Iterator::js_some);
        ctor.definePrototypeMethod(scope, "every", 1, NativeES2025Iterator::js_every);
        ctor.definePrototypeMethod(scope, "find", 1, NativeES2025Iterator::js_find);

        proto.defineProperty(
                cx,
                "constructor",
                (thisObj) -> ctor,
                (owner, value) ->
                        setterThatIgnoresPrototypeProperties(owner, proto, "constructor", value),
                DONTENUM);
        proto.defineProperty(
                cx,
                SymbolKey.TO_STRING_TAG,
                (thisObj) -> CLASS_NAME,
                (owner, value) ->
                        setterThatIgnoresPrototypeProperties(
                                owner, proto, SymbolKey.TO_STRING_TAG, value),
                DONTENUM);

        ScriptableObject.defineProperty(scope, CLASS_NAME, ctor, DONTENUM);
        if (sealed) {
            ctor.sealObject();
        }
        finishPrototype(scope, proto, ITERATOR_PROTOTYPE_TAG, sealed);

        NativeObject helperProto = newPrototype(scope, proto);
        defineMethod(scope, helperProto, "next", 0, NativeES2025Iterator::js_helperNext);
        defineMethod(scope, helperProto, "return", 0, NativeES2025Iterator::js_helperReturn);
        helperProto.defineProperty(SymbolKey.TO_STRING_TAG, HELPER_TAG, DONTENUM | READONLY);
        finishPrototype(scope, helperProto, HELPER_PROTOTYPE_TAG, sealed);

        NativeObject wrapProto = newPrototype(scope, proto);
        defineMethod(scope, wrapProto, "next", 0, NativeES2025Iterator::js_wrapNext);
        defineMethod(scope, wrapProto, "return", 0, NativeES2025Iterator::js_wrapReturn);
        finishPrototype(scope, wrapProto, WRAP_PROTOTYPE_TAG, sealed);
    }

    private static NativeObject newPrototype(ScriptableObject scope, Scriptable parentProto) {
        NativeObject proto = new NativeObject();
        proto.setParentScope(scope);
        proto.setPrototype(parentProto);
        return proto;
    }

    private static void defineMethod(
            Scriptable scope,
            ScriptableObject obj,
            String name,
            int length,
            SerializableCallable target) {
        obj.defineProperty(name, new LambdaFunction(scope, name, length, target), DONTENUM);
    }

    private static void defineMethod(
            Scriptable scope,
            ScriptableObject obj,
            SymbolKey key,
            int length,
            SerializableCallable target) {
        obj.defineProperty(
                key,
                new LambdaFunction(scope, "[" + key.getName() + "]", length, target),
                DONTENUM);
    }

    private static void finishPrototype(
            ScriptableObject scope, ScriptableObject proto, Object tag, boolean sealed) {
        if (sealed) {
            proto.sealObject();
        }
        scope.associateValue(tag, proto);
    }

    /** Returns %Iterator.prototype% of the given scope, or null if it has not been initialized. */
    static Scriptable getIteratorPrototype(Scriptable scope) {
        Scriptable top = ScriptableObject.getTopLevelScope(scope);
        return (Scriptable) ScriptableObject.getTopScopeValue(top, ITERATOR_PROTOTYPE_TAG);
    }

    private NativeES2025Iterator() {}

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    private static void setterThatIgnoresPrototypeProperties(
            Scriptable thisObj, Scriptable home, Object key, Object value) {
        if (!ScriptRuntime.isObject(thisObj)) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(thisObj));
        }
        if (thisObj == home) {
            throw ScriptRuntime.typeErrorById("msg.iterator.proto.assign", key.toString());
        }
        boolean hasOwn =
                key instanceof Symbol
                        ? ((SymbolScriptable) thisObj).has((Symbol) key, thisObj)
                        : thisObj.has((String) key, thisObj);
        if (!hasOwn && thisObj instanceof ScriptableObject) {
            ((ScriptableObject) thisObj)
                    .defineOwnProperty(
                            Context.getCurrentContext(),
                            key,
                            new DescriptorInfo(true, true, true, value));
        } else if (key instanceof Symbol) {
            ScriptableObject.putProperty(thisObj, (Symbol) key, value);
        } else {
            ScriptableObject.putProperty(thisObj, (String) key, value);
        }
    }

    private static Object js_abstract(Context cx, Scriptable s, Scriptable thisObj, Object[] args) {
        throw ScriptRuntime.typeErrorById("msg.iterator.abstract");
    }

    private static Scriptable js_construct(Context cx, Scriptable s, Object[] args) {
        throw ScriptRuntime.typeErrorById("msg.iterator.abstract");
    }

    private static Object js_iterator(Context cx, Scriptable s, Scriptable thisObj, Object[] args) {
        return thisObj;
    }

    // Iterator.from

    private static Object js_from(Context cx, Scriptable s, Scriptable thisObj, Object[] args) {
        Object o = args.length > 0 ? args[0] : Undefined.instance;
        IteratorRecord record = getIteratorFlattenable(cx, s, o, true);
        Scriptable iteratorProto = getIteratorPrototype(s);
        for (Scriptable p = record.iterator.getPrototype(); p != null; p = p.getPrototype()) {
            if (p == iteratorProto) {
                return record.iterator;
            }
        }
        WrapForValidIterator wrapper = new WrapForValidIterator(record);
        Scriptable top = ScriptableObject.getTopLevelScope(s);
        wrapper.setParentScope(top);
        wrapper.setPrototype(
                (Scriptable) ScriptableObject.getTopScopeValue(top, WRAP_PROTOTYPE_TAG));
        return wrapper;
    }

    private static Object js_wrapNext(Context cx, Scriptable s, Scriptable thisObj, Object[] args) {
        IteratorRecord record =
                LambdaConstructor.convertThisObject(thisObj, WrapForValidIterator.class).iterated;
        return record.callNext(cx, s);
    }

    private static Object js_wrapReturn(
            Context cx, Scriptable s, Scriptable thisObj, Object[] args) {
        IteratorRecord record =
                LambdaConstructor.convertThisObject(thisObj, WrapForValidIterator.class).iterated;
        Callable returnMethod = getReturnMethod(cx, s, record.iterator);
        if (returnMethod == null) {
            return ES6Iterator.makeIteratorResult(cx, s, Boolean.TRUE);
        }
        return returnMethod.call(cx, s, record.iterator, ScriptRuntime.emptyArgs);
    }

    // Iterator.concat

    private static Object js_concat(Context cx, Scriptable s, Scriptable thisObj, Object[] args) {
        Object[] methods = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object item = args[i];
            if (!ScriptRuntime.isObject(item)) {
                throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(item));
            }
            Object method = ScriptableObject.getProperty((Scriptable) item, SymbolKey.ITERATOR);
            if (method == NOT_FOUND || method == null || Undefined.isUndefined(method)) {
                throw ScriptRuntime.typeErrorById("msg.not.iterable", ScriptRuntime.toString(item));
            }
            if (!(method instanceof Callable)) {
                throw ScriptRuntime.notFunctionError(item, method, "Symbol.iterator");
            }
            methods[i] = method;
        }
        return initHelper(s, new ConcatHelper(methods, args.clone()));
    }

    // Lazy iterator helpers

    private static Object js_map(Context cx, Scriptable s, Scriptable thisObj, Object[] args) {
        Scriptable o = requireObject(thisObj);
        Callable mapper = requireCallable(cx, s, o, args);
        return initHelper(s, new MapHelper(IteratorRecord.direct(cx, s, o), mapper));
    }

    private static Object js_filter(Context cx, Scriptable s, Scriptable thisObj, Object[] args) {
        Scriptable o = requireObject(thisObj);
        Callable predicate = requireCallable(cx, s, o, args);
        return initHelper(s, new FilterHelper(IteratorRecord.direct(cx, s, o), predicate));
    }

    private static Object js_take(Context cx, Scriptable s, Scriptable thisObj, Object[] args) {
        Scriptable o = requireObject(thisObj);
        double limit = requireLimit(cx, s, o, args);
        return initHelper(s, new TakeHelper(IteratorRecord.direct(cx, s, o), limit));
    }

    private static Object js_drop(Context cx, Scriptable s, Scriptable thisObj, Object[] args) {
        Scriptable o = requireObject(thisObj);
        double limit = requireLimit(cx, s, o, args);
        return initHelper(s, new DropHelper(IteratorRecord.direct(cx, s, o), limit));
    }

    private static Object js_flatMap(Context cx, Scriptable s, Scriptable thisObj, Object[] args) {
        Scriptable o = requireObject(thisObj);
        Callable mapper = requireCallable(cx, s, o, args);
        return initHelper(s, new FlatMapHelper(IteratorRecord.direct(cx, s, o), mapper));
    }

    // Eager iterator helpers

    private static Object js_reduce(Context cx, Scriptable s, Scriptable thisObj, Object[] args) {
        Scriptable o = requireObject(thisObj);
        Callable reducer = requireCallable(cx, s, o, args);
        IteratorRecord iterated = IteratorRecord.direct(cx, s, o);
        Object accumulator;
        long counter;
        if (args.length < 2) {
            accumulator = iterated.stepValue(cx, s);
            if (accumulator == DONE) {
                throw ScriptRuntime.typeErrorById("msg.iterator.reduce.empty");
            }
            counter = 1;
        } else {
            accumulator = args[1];
            counter = 0;
        }
        while (true) {
            Object value = iterated.stepValue(cx, s);
            if (value == DONE) {
                return accumulator;
            }
            accumulator =
                    callAndCloseOnError(
                            cx, s, iterated, reducer, accumulator, value, (double) counter);
            counter++;
        }
    }

    private static Object js_toArray(Context cx, Scriptable s, Scriptable thisObj, Object[] args) {
        Scriptable o = requireObject(thisObj);
        IteratorRecord iterated = IteratorRecord.direct(cx, s, o);
        ArrayList<Object> items = new ArrayList<>();
        while (true) {
            Object value = iterated.stepValue(cx, s);
            if (value == DONE) {
                return cx.newArray(s, items.toArray());
            }
            items.add(value);
        }
    }

    private static Object js_forEach(Context cx, Scriptable s, Scriptable thisObj, Object[] args) {
        Scriptable o = requireObject(thisObj);
        Callable fn = requireCallable(cx, s, o, args);
        IteratorRecord iterated = IteratorRecord.direct(cx, s, o);
        long counter = 0;
        while (true) {
            Object value = iterated.stepValue(cx, s);
            if (value == DONE) {
                return Undefined.instance;
            }
            callAndCloseOnError(cx, s, iterated, fn, value, (double) counter);
            counter++;
        }
    }

    private static Object js_some(Context cx, Scriptable s, Scriptable thisObj, Object[] args) {
        Scriptable o = requireObject(thisObj);
        Callable predicate = requireCallable(cx, s, o, args);
        IteratorRecord iterated = IteratorRecord.direct(cx, s, o);
        long counter = 0;
        while (true) {
            Object value = iterated.stepValue(cx, s);
            if (value == DONE) {
                return Boolean.FALSE;
            }
            Object result =
                    callAndCloseOnError(cx, s, iterated, predicate, value, (double) counter);
            if (ScriptRuntime.toBoolean(result)) {
                iterated.close(cx, s);
                return Boolean.TRUE;
            }
            counter++;
        }
    }

    private static Object js_every(Context cx, Scriptable s, Scriptable thisObj, Object[] args) {
        Scriptable o = requireObject(thisObj);
        Callable predicate = requireCallable(cx, s, o, args);
        IteratorRecord iterated = IteratorRecord.direct(cx, s, o);
        long counter = 0;
        while (true) {
            Object value = iterated.stepValue(cx, s);
            if (value == DONE) {
                return Boolean.TRUE;
            }
            Object result =
                    callAndCloseOnError(cx, s, iterated, predicate, value, (double) counter);
            if (!ScriptRuntime.toBoolean(result)) {
                iterated.close(cx, s);
                return Boolean.FALSE;
            }
            counter++;
        }
    }

    private static Object js_find(Context cx, Scriptable s, Scriptable thisObj, Object[] args) {
        Scriptable o = requireObject(thisObj);
        Callable predicate = requireCallable(cx, s, o, args);
        IteratorRecord iterated = IteratorRecord.direct(cx, s, o);
        long counter = 0;
        while (true) {
            Object value = iterated.stepValue(cx, s);
            if (value == DONE) {
                return Undefined.instance;
            }
            Object result =
                    callAndCloseOnError(cx, s, iterated, predicate, value, (double) counter);
            if (ScriptRuntime.toBoolean(result)) {
                iterated.close(cx, s);
                return value;
            }
            counter++;
        }
    }

    // %IteratorHelperPrototype%

    private static Object js_helperNext(
            Context cx, Scriptable s, Scriptable thisObj, Object[] args) {
        IteratorHelper helper = LambdaConstructor.convertThisObject(thisObj, IteratorHelper.class);
        switch (helper.state) {
            case EXECUTING:
                throw ScriptRuntime.typeErrorById("msg.generator.executing");
            case COMPLETED:
                return ES6Iterator.makeIteratorResult(cx, s, Boolean.TRUE);
            default:
                break;
        }
        helper.state = State.EXECUTING;
        Object value = DONE;
        try {
            value = helper.step(cx, s);
        } finally {
            helper.state = value == DONE ? State.COMPLETED : State.SUSPENDED_YIELD;
        }
        if (value == DONE) {
            return ES6Iterator.makeIteratorResult(cx, s, Boolean.TRUE);
        }
        return ES6Iterator.makeIteratorResult(cx, s, Boolean.FALSE, value);
    }

    private static Object js_helperReturn(
            Context cx, Scriptable s, Scriptable thisObj, Object[] args) {
        IteratorHelper helper = LambdaConstructor.convertThisObject(thisObj, IteratorHelper.class);
        switch (helper.state) {
            case SUSPENDED_START:
                helper.state = State.COMPLETED;
                if (helper.underlying != null) {
                    helper.underlying.close(cx, s);
                }
                break;
            case SUSPENDED_YIELD:
                helper.state = State.EXECUTING;
                try {
                    helper.closeAfterYield(cx, s);
                } finally {
                    helper.state = State.COMPLETED;
                }
                break;
            case EXECUTING:
                throw ScriptRuntime.typeErrorById("msg.generator.executing");
            case COMPLETED:
                break;
        }
        return ES6Iterator.makeIteratorResult(cx, s, Boolean.TRUE);
    }

    // Abstract operations

    private static Scriptable requireObject(Object thisObj) {
        if (!ScriptRuntime.isObject(thisObj)) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(thisObj));
        }
        return (Scriptable) thisObj;
    }

    private static Callable requireCallable(Context cx, Scriptable s, Scriptable o, Object[] args) {
        Object fn = args.length > 0 ? args[0] : Undefined.instance;
        if (!(fn instanceof Callable)) {
            throw closeOnError(cx, s, o, ScriptRuntime.notFunctionError(fn));
        }
        return (Callable) fn;
    }

    private static double requireLimit(Context cx, Scriptable s, Scriptable o, Object[] args) {
        double limit;
        try {
            limit = ScriptRuntime.toNumber(args.length > 0 ? args[0] : Undefined.instance);
        } catch (RhinoException e) {
            throw closeOnError(cx, s, o, e);
        }
        if (Double.isNaN(limit)) {
            throw closeOnError(
                    cx, s, o, ScriptRuntime.rangeErrorById("msg.iterator.limit.range", "NaN"));
        }
        limit = ScriptRuntime.toIntegerOrInfinity(limit);
        if (limit < 0) {
            throw closeOnError(
                    cx,
                    s,
                    o,
                    ScriptRuntime.rangeErrorById(
                            "msg.iterator.limit.range", ScriptRuntime.toString(limit)));
        }
        return limit;
    }

    private static IteratorRecord getIteratorFlattenable(
            Context cx, Scriptable s, Object obj, boolean iterateStrings) {
        Object iterator;
        if (!ScriptRuntime.isObject(obj)) {
            if (!iterateStrings || !(obj instanceof CharSequence)) {
                throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(obj));
            }
            iterator = ScriptRuntime.callIterator(obj, cx, s);
        } else {
            Object method = ScriptableObject.getProperty((Scriptable) obj, SymbolKey.ITERATOR);
            if (method == NOT_FOUND || method == null || Undefined.isUndefined(method)) {
                iterator = obj;
            } else if (method instanceof Callable) {
                iterator =
                        ((Callable) method).call(cx, s, (Scriptable) obj, ScriptRuntime.emptyArgs);
            } else {
                throw ScriptRuntime.notFunctionError(obj, method, "Symbol.iterator");
            }
        }
        if (!ScriptRuntime.isObject(iterator)) {
            throw ScriptRuntime.typeErrorById("msg.arg.not.object", ScriptRuntime.typeof(iterator));
        }
        return IteratorRecord.direct(cx, s, (Scriptable) iterator);
    }

    private static Callable getReturnMethod(Context cx, Scriptable s, Scriptable iterator) {
        Object method =
                ScriptRuntime.getObjectPropNoWarn(iterator, ES6Iterator.RETURN_METHOD, cx, s);
        if (method == null || Undefined.isUndefined(method)) {
            return null;
        }
        if (!(method instanceof Callable)) {
            throw ScriptRuntime.notFunctionError(iterator, method, ES6Iterator.RETURN_METHOD);
        }
        return (Callable) method;
    }

    /** IteratorClose with a throw completion: errors from "return" are ignored. */
    private static RuntimeException closeOnError(
            Context cx, Scriptable s, Scriptable iterator, RuntimeException error) {
        try {
            Callable returnMethod = getReturnMethod(cx, s, iterator);
            if (returnMethod != null) {
                returnMethod.call(cx, s, iterator, ScriptRuntime.emptyArgs);
            }
        } catch (RhinoException ignored) {
            // the original error takes precedence
        }
        return error;
    }

    private static Object callAndCloseOnError(
            Context cx, Scriptable s, IteratorRecord iterated, Callable fn, Object... args) {
        try {
            Scriptable thisObj = ScriptRuntime.getApplyOrCallThis(cx, s, Undefined.instance, 1, fn);
            return fn.call(cx, s, thisObj, args);
        } catch (RhinoException e) {
            throw closeOnError(cx, s, iterated.iterator, e);
        }
    }

    private static IteratorHelper initHelper(Scriptable s, IteratorHelper helper) {
        Scriptable top = ScriptableObject.getTopLevelScope(s);
        helper.setParentScope(top);
        helper.setPrototype(
                (Scriptable) ScriptableObject.getTopScopeValue(top, HELPER_PROTOTYPE_TAG));
        return helper;
    }

    private static final class IteratorRecord implements Serializable {
        private static final long serialVersionUID = -2546253862036924553L;

        final Scriptable iterator;
        final Object nextMethod;

        private IteratorRecord(Scriptable iterator, Object nextMethod) {
            this.iterator = iterator;
            this.nextMethod = nextMethod;
        }

        private void readObject(ObjectInputStream stream)
                throws IOException, ClassNotFoundException {
            Context.checkDeserializationEnabled();
            stream.defaultReadObject();
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            Context.checkSerializationEnabled();
            stream.defaultWriteObject();
        }

        private void readObjectNoData() throws InvalidObjectException {
            throw new InvalidObjectException("Missing serialized Rhino object data");
        }

        /** GetIteratorDirect */
        static IteratorRecord direct(Context cx, Scriptable s, Scriptable obj) {
            return new IteratorRecord(
                    obj, ScriptRuntime.getObjectPropNoWarn(obj, ES6Iterator.NEXT_METHOD, cx, s));
        }

        Object callNext(Context cx, Scriptable s) {
            if (!(nextMethod instanceof Callable)) {
                throw ScriptRuntime.notFunctionError(iterator, nextMethod, ES6Iterator.NEXT_METHOD);
            }
            return ((Callable) nextMethod).call(cx, s, iterator, ScriptRuntime.emptyArgs);
        }

        /** IteratorStep: returns null if the iterator is done, the result object otherwise. */
        Scriptable step(Context cx, Scriptable s) {
            Object result = callNext(cx, s);
            if (!ScriptRuntime.isObject(result)) {
                throw ScriptRuntime.typeErrorById(
                        "msg.arg.not.object", ScriptRuntime.typeof(result));
            }
            Object done =
                    ScriptRuntime.getObjectPropNoWarn(result, ES6Iterator.DONE_PROPERTY, cx, s);
            return ScriptRuntime.toBoolean(done) ? null : (Scriptable) result;
        }

        /** IteratorStepValue: returns {@link #DONE} if the iterator is done. */
        Object stepValue(Context cx, Scriptable s) {
            Scriptable result = step(cx, s);
            if (result == null) {
                return DONE;
            }
            return ScriptRuntime.getObjectPropNoWarn(result, ES6Iterator.VALUE_PROPERTY, cx, s);
        }

        /** IteratorClose with a normal completion. */
        void close(Context cx, Scriptable s) {
            Callable returnMethod = getReturnMethod(cx, s, iterator);
            if (returnMethod == null) {
                return;
            }
            Object result = returnMethod.call(cx, s, iterator, ScriptRuntime.emptyArgs);
            if (!ScriptRuntime.isObject(result)) {
                throw ScriptRuntime.typeErrorById(
                        "msg.arg.not.object", ScriptRuntime.typeof(result));
            }
        }
    }

    private static final class WrapForValidIterator extends ScriptableObject {
        private static final long serialVersionUID = 2187213064522745096L;

        final IteratorRecord iterated;

        WrapForValidIterator(IteratorRecord iterated) {
            this.iterated = iterated;
        }

        @Override
        public String getClassName() {
            return "Object";
        }
    }

    private enum State {
        SUSPENDED_START,
        SUSPENDED_YIELD,
        EXECUTING,
        COMPLETED
    }

    private abstract static class IteratorHelper extends ScriptableObject {
        private static final long serialVersionUID = -7290950125245226862L;

        final IteratorRecord underlying;
        State state = State.SUSPENDED_START;

        IteratorHelper(IteratorRecord underlying) {
            this.underlying = underlying;
        }

        @Override
        public String getClassName() {
            return "Object";
        }

        /** Runs the closure until the next yield. Returns {@link #DONE} when it completes. */
        abstract Object step(Context cx, Scriptable s);

        /** Handles a return completion received at a yield. */
        void closeAfterYield(Context cx, Scriptable s) {
            underlying.close(cx, s);
        }
    }

    private static final class MapHelper extends IteratorHelper {
        private static final long serialVersionUID = 1L;

        private final Callable mapper;
        private long counter;

        MapHelper(IteratorRecord underlying, Callable mapper) {
            super(underlying);
            this.mapper = mapper;
        }

        @Override
        Object step(Context cx, Scriptable s) {
            Object value = underlying.stepValue(cx, s);
            if (value == DONE) {
                return DONE;
            }
            return callAndCloseOnError(cx, s, underlying, mapper, value, (double) counter++);
        }
    }

    private static final class FilterHelper extends IteratorHelper {
        private static final long serialVersionUID = 1L;

        private final Callable predicate;
        private long counter;

        FilterHelper(IteratorRecord underlying, Callable predicate) {
            super(underlying);
            this.predicate = predicate;
        }

        @Override
        Object step(Context cx, Scriptable s) {
            while (true) {
                Object value = underlying.stepValue(cx, s);
                if (value == DONE) {
                    return DONE;
                }
                Object selected =
                        callAndCloseOnError(
                                cx, s, underlying, predicate, value, (double) counter++);
                if (ScriptRuntime.toBoolean(selected)) {
                    return value;
                }
            }
        }
    }

    private static final class TakeHelper extends IteratorHelper {
        private static final long serialVersionUID = 1L;

        private double remaining;

        TakeHelper(IteratorRecord underlying, double limit) {
            super(underlying);
            this.remaining = limit;
        }

        @Override
        Object step(Context cx, Scriptable s) {
            if (remaining == 0) {
                underlying.close(cx, s);
                return DONE;
            }
            if (remaining != Double.POSITIVE_INFINITY) {
                remaining--;
            }
            return underlying.stepValue(cx, s);
        }
    }

    private static final class DropHelper extends IteratorHelper {
        private static final long serialVersionUID = 1L;

        private double remaining;

        DropHelper(IteratorRecord underlying, double limit) {
            super(underlying);
            this.remaining = limit;
        }

        @Override
        Object step(Context cx, Scriptable s) {
            while (remaining > 0) {
                if (remaining != Double.POSITIVE_INFINITY) {
                    remaining--;
                }
                if (underlying.step(cx, s) == null) {
                    return DONE;
                }
            }
            return underlying.stepValue(cx, s);
        }
    }

    private static final class FlatMapHelper extends IteratorHelper {
        private static final long serialVersionUID = 1L;

        private final Callable mapper;
        private long counter;
        private IteratorRecord inner;

        FlatMapHelper(IteratorRecord underlying, Callable mapper) {
            super(underlying);
            this.mapper = mapper;
        }

        @Override
        Object step(Context cx, Scriptable s) {
            while (true) {
                if (inner == null) {
                    Object value = underlying.stepValue(cx, s);
                    if (value == DONE) {
                        return DONE;
                    }
                    Object mapped =
                            callAndCloseOnError(
                                    cx, s, underlying, mapper, value, (double) counter++);
                    try {
                        inner = getIteratorFlattenable(cx, s, mapped, false);
                    } catch (RhinoException e) {
                        throw closeOnError(cx, s, underlying.iterator, e);
                    }
                }
                Object innerValue;
                try {
                    innerValue = inner.stepValue(cx, s);
                } catch (RhinoException e) {
                    throw closeOnError(cx, s, underlying.iterator, e);
                }
                if (innerValue != DONE) {
                    return innerValue;
                }
                inner = null;
            }
        }

        @Override
        void closeAfterYield(Context cx, Scriptable s) {
            try {
                inner.close(cx, s);
            } catch (RhinoException e) {
                throw closeOnError(cx, s, underlying.iterator, e);
            }
            underlying.close(cx, s);
        }
    }

    private static final class ConcatHelper extends IteratorHelper {
        private static final long serialVersionUID = 1L;

        private final Object[] methods;
        private final Object[] iterables;
        private int index;
        private IteratorRecord current;

        ConcatHelper(Object[] methods, Object[] iterables) {
            super(null);
            this.methods = methods;
            this.iterables = iterables;
        }

        @Override
        Object step(Context cx, Scriptable s) {
            while (true) {
                if (current == null) {
                    if (index >= methods.length) {
                        return DONE;
                    }
                    Object iter =
                            ((Callable) methods[index])
                                    .call(
                                            cx,
                                            s,
                                            (Scriptable) iterables[index],
                                            ScriptRuntime.emptyArgs);
                    index++;
                    if (!ScriptRuntime.isObject(iter)) {
                        throw ScriptRuntime.typeErrorById(
                                "msg.arg.not.object", ScriptRuntime.typeof(iter));
                    }
                    current = IteratorRecord.direct(cx, s, (Scriptable) iter);
                }
                Object value = current.stepValue(cx, s);
                if (value != DONE) {
                    return value;
                }
                current = null;
            }
        }

        @Override
        void closeAfterYield(Context cx, Scriptable s) {
            current.close(cx, s);
        }
    }
}
