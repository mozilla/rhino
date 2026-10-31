/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.mozilla.javascript.ClassDescriptor.Builder.value;
import static org.mozilla.javascript.ClassDescriptor.Destination.CTOR;
import static org.mozilla.javascript.ClassDescriptor.Destination.PROTO;
import static org.mozilla.javascript.Symbol.Kind.REGULAR;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
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
    @Serial private static final long serialVersionUID = 7614590447128843560L;

    private static final String CLASS_NAME = "Iterator";
    private static final String HELPER_TAG = "Iterator Helper";

    static final SymbolKey ITERATOR_PROTOTYPE_TAG = new SymbolKey("IteratorPrototype", REGULAR);
    private static final SymbolKey HELPER_PROTOTYPE_TAG =
            new SymbolKey("IteratorHelperPrototype", REGULAR);
    private static final SymbolKey WRAP_PROTOTYPE_TAG =
            new SymbolKey("WrapForValidIteratorPrototype", REGULAR);

    private static final Object DONE = new Object();

    private static final ClassDescriptor DESCRIPTOR;
    private static final ClassDescriptor HIDDEN_PROTO_DESCRIPTOR;
    private static final ClassDescriptor HELPER_DESCRIPTOR;
    private static final ClassDescriptor WRAP_DESCRIPTOR;

    static {
        DESCRIPTOR =
                new ClassDescriptor.Builder(
                                CLASS_NAME,
                                0,
                                ClassDescriptor.typeError(),
                                NativeES2025Iterator::js_constructor)
                        .withMethod(CTOR, "from", 1, NativeES2025Iterator::js_from)
                        .withMethod(CTOR, "concat", 0, NativeES2025Iterator::js_concat)
                        .withMethod(PROTO, SymbolKey.ITERATOR, 0, NativeES2025Iterator::js_iterator)
                        .withMethod(PROTO, "map", 1, NativeES2025Iterator::js_map)
                        .withMethod(PROTO, "filter", 1, NativeES2025Iterator::js_filter)
                        .withMethod(PROTO, "take", 1, NativeES2025Iterator::js_take)
                        .withMethod(PROTO, "drop", 1, NativeES2025Iterator::js_drop)
                        .withMethod(PROTO, "flatMap", 1, NativeES2025Iterator::js_flatMap)
                        .withMethod(PROTO, "reduce", 1, NativeES2025Iterator::js_reduce)
                        .withMethod(PROTO, "toArray", 0, NativeES2025Iterator::js_toArray)
                        .withMethod(PROTO, "forEach", 1, NativeES2025Iterator::js_forEach)
                        .withMethod(PROTO, "some", 1, NativeES2025Iterator::js_some)
                        .withMethod(PROTO, "every", 1, NativeES2025Iterator::js_every)
                        .withMethod(PROTO, "find", 1, NativeES2025Iterator::js_find)
                        .build();
        HIDDEN_PROTO_DESCRIPTOR =
                new ClassDescriptor.Builder(ITERATOR_PROTOTYPE_TAG)
                        .withMethod(CTOR, SymbolKey.ITERATOR, 0, NativeES2025Iterator::js_iterator)
                        .build();
        HELPER_DESCRIPTOR =
                new ClassDescriptor.Builder(HELPER_PROTOTYPE_TAG)
                        .withMethod(CTOR, "next", 0, NativeES2025Iterator::js_helperNext)
                        .withMethod(CTOR, "return", 0, NativeES2025Iterator::js_helperReturn)
                        .withProp(
                                CTOR,
                                SymbolKey.TO_STRING_TAG,
                                value(HELPER_TAG, DONTENUM | READONLY))
                        .build();
        WRAP_DESCRIPTOR =
                new ClassDescriptor.Builder(WRAP_PROTOTYPE_TAG)
                        .withMethod(CTOR, "next", 0, NativeES2025Iterator::js_wrapNext)
                        .withMethod(CTOR, "return", 0, NativeES2025Iterator::js_wrapReturn)
                        .build();
    }

    static void init(Context cx, TopLevel scope, boolean sealed) {
        if (cx.getLanguageVersion() < Context.VERSION_ECMASCRIPT) {
            var proto =
                    HIDDEN_PROTO_DESCRIPTOR.populateGlobal(cx, scope, new NativeObject(), sealed);
            scope.associateValue(ITERATOR_PROTOTYPE_TAG, proto);
            return;
        }

        NativeObject proto = new NativeObject();
        DESCRIPTOR.buildConstructor(
                cx,
                scope,
                proto,
                sealed,
                (c, ctor) -> {
                    proto.defineProperty(
                            c,
                            scope,
                            "constructor",
                            (thisObj) -> ctor,
                            (owner, value) ->
                                    setterThatIgnoresPrototypeProperties(
                                            owner, proto, "constructor", value),
                            DONTENUM);
                    proto.defineProperty(
                            c,
                            scope,
                            SymbolKey.TO_STRING_TAG,
                            (thisObj) -> CLASS_NAME,
                            (owner, value) ->
                                    setterThatIgnoresPrototypeProperties(
                                            owner, proto, SymbolKey.TO_STRING_TAG, value),
                            DONTENUM);
                });
        scope.associateValue(ITERATOR_PROTOTYPE_TAG, proto);

        var helperProto = HELPER_DESCRIPTOR.populateGlobal(cx, scope, new NativeObject(), sealed);
        helperProto.setPrototype(proto);
        scope.associateValue(HELPER_PROTOTYPE_TAG, helperProto);

        var wrapProto = WRAP_DESCRIPTOR.populateGlobal(cx, scope, new NativeObject(), sealed);
        wrapProto.setPrototype(proto);
        scope.associateValue(WRAP_PROTOTYPE_TAG, wrapProto);
    }

    /** Returns %Iterator.prototype% of the given scope, or null if it has not been initialized. */
    static Scriptable getIteratorPrototype(VarScope scope) {
        TopLevel top = ScriptableObject.getTopLevelScope(scope);
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

    private static Scriptable js_constructor(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        if (nt == null || Undefined.isUndefined(nt) || nt == f) {
            throw ScriptRuntime.typeErrorById("msg.iterator.abstract");
        }
        NativeObject result = new NativeObject();
        Object proto = ((Scriptable) nt).get("prototype", (Scriptable) nt);
        result.setPrototype(
                proto instanceof Scriptable && ScriptRuntime.isObject(proto)
                        ? (Scriptable) proto
                        : getIteratorPrototype(s));
        result.setParentScope(s);
        return result;
    }

    private static Object js_iterator(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return thisObj;
    }

    // Iterator.from

    private static Object js_from(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        Object o = args.length > 0 ? args[0] : Undefined.instance;
        IteratorRecord record = getIteratorFlattenable(cx, s, o, true);
        Scriptable iteratorProto = getIteratorPrototype(s);
        for (Scriptable p = record.iterator.getPrototype(); p != null; p = p.getPrototype()) {
            if (p == iteratorProto) {
                return record.iterator;
            }
        }
        WrapForValidIterator wrapper = new WrapForValidIterator(record);
        TopLevel top = ScriptableObject.getTopLevelScope(s);
        wrapper.setParentScope(top);
        wrapper.setPrototype(
                (Scriptable) ScriptableObject.getTopScopeValue(top, WRAP_PROTOTYPE_TAG));
        return wrapper;
    }

    private static Object js_wrapNext(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        IteratorRecord record =
                LambdaConstructor.convertThisObject(thisObj, WrapForValidIterator.class).iterated;
        return record.callNext(cx, s);
    }

    private static Object js_wrapReturn(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        IteratorRecord record =
                LambdaConstructor.convertThisObject(thisObj, WrapForValidIterator.class).iterated;
        Callable returnMethod = getReturnMethod(cx, s, record.iterator);
        if (returnMethod == null) {
            return ES6Iterator.makeIteratorResult(cx, s, Boolean.TRUE);
        }
        return returnMethod.call(cx, s, record.iterator, ScriptRuntime.emptyArgs);
    }

    // Iterator.concat

    private static Object js_concat(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
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

    private static Object js_map(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        Scriptable o = requireObject(thisObj);
        Callable mapper = requireCallable(cx, s, o, args);
        return initHelper(s, new MapHelper(IteratorRecord.direct(cx, s, o), mapper));
    }

    private static Object js_filter(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        Scriptable o = requireObject(thisObj);
        Callable predicate = requireCallable(cx, s, o, args);
        return initHelper(s, new FilterHelper(IteratorRecord.direct(cx, s, o), predicate));
    }

    private static Object js_take(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        Scriptable o = requireObject(thisObj);
        double limit = requireLimit(cx, s, o, args);
        return initHelper(s, new TakeHelper(IteratorRecord.direct(cx, s, o), limit));
    }

    private static Object js_drop(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        Scriptable o = requireObject(thisObj);
        double limit = requireLimit(cx, s, o, args);
        return initHelper(s, new DropHelper(IteratorRecord.direct(cx, s, o), limit));
    }

    private static Object js_flatMap(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        Scriptable o = requireObject(thisObj);
        Callable mapper = requireCallable(cx, s, o, args);
        return initHelper(s, new FlatMapHelper(IteratorRecord.direct(cx, s, o), mapper));
    }

    // Eager iterator helpers

    private static Object js_reduce(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
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

    private static Object js_toArray(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
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

    private static Object js_forEach(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
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

    private static Object js_some(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
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

    private static Object js_every(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
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

    private static Object js_find(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
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
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
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
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
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

    private static Callable requireCallable(Context cx, VarScope s, Scriptable o, Object[] args) {
        Object fn = args.length > 0 ? args[0] : Undefined.instance;
        if (!(fn instanceof Callable)) {
            throw closeOnError(cx, s, o, ScriptRuntime.notFunctionError(fn));
        }
        return (Callable) fn;
    }

    private static double requireLimit(Context cx, VarScope s, Scriptable o, Object[] args) {
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
            Context cx, VarScope s, Object obj, boolean iterateStrings) {
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

    private static Callable getReturnMethod(Context cx, VarScope s, Scriptable iterator) {
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
            Context cx, VarScope s, Scriptable iterator, RuntimeException error) {
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
            Context cx, VarScope s, IteratorRecord iterated, Callable fn, Object... args) {
        try {
            return fn.call(cx, s, Undefined.SCRIPTABLE_UNDEFINED, args);
        } catch (RhinoException e) {
            throw closeOnError(cx, s, iterated.iterator, e);
        }
    }

    private static IteratorHelper initHelper(VarScope s, IteratorHelper helper) {
        TopLevel top = ScriptableObject.getTopLevelScope(s);
        helper.setParentScope(top);
        helper.setPrototype(
                (Scriptable) ScriptableObject.getTopScopeValue(top, HELPER_PROTOTYPE_TAG));
        return helper;
    }

    private static final class IteratorRecord implements Serializable {
        @Serial private static final long serialVersionUID = -2546253862036924553L;

        final Scriptable iterator;
        final Object nextMethod;

        private IteratorRecord(Scriptable iterator, Object nextMethod) {
            this.iterator = iterator;
            this.nextMethod = nextMethod;
        }

        @Serial
        private void readObject(ObjectInputStream stream)
                throws IOException, ClassNotFoundException {
            Context.checkDeserializationEnabled();
            stream.defaultReadObject();
        }

        @Serial
        private void writeObject(ObjectOutputStream stream) throws IOException {
            Context.checkSerializationEnabled();
            stream.defaultWriteObject();
        }

        @Serial
        private void readObjectNoData() throws InvalidObjectException {
            throw new InvalidObjectException("Missing serialized Rhino object data");
        }

        /** GetIteratorDirect */
        static IteratorRecord direct(Context cx, VarScope s, Scriptable obj) {
            return new IteratorRecord(
                    obj, ScriptRuntime.getObjectPropNoWarn(obj, ES6Iterator.NEXT_METHOD, cx, s));
        }

        Object callNext(Context cx, VarScope s) {
            if (!(nextMethod instanceof Callable)) {
                throw ScriptRuntime.notFunctionError(iterator, nextMethod, ES6Iterator.NEXT_METHOD);
            }
            return ((Callable) nextMethod).call(cx, s, iterator, ScriptRuntime.emptyArgs);
        }

        /** IteratorStep: returns null if the iterator is done, the result object otherwise. */
        Scriptable step(Context cx, VarScope s) {
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
        Object stepValue(Context cx, VarScope s) {
            Scriptable result = step(cx, s);
            if (result == null) {
                return DONE;
            }
            return ScriptRuntime.getObjectPropNoWarn(result, ES6Iterator.VALUE_PROPERTY, cx, s);
        }

        /** IteratorClose with a normal completion. */
        void close(Context cx, VarScope s) {
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
        @Serial private static final long serialVersionUID = 2187213064522745096L;

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
        @Serial private static final long serialVersionUID = -7290950125245226862L;

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
        abstract Object step(Context cx, VarScope s);

        /** Handles a return completion received at a yield. */
        void closeAfterYield(Context cx, VarScope s) {
            underlying.close(cx, s);
        }
    }

    private static final class MapHelper extends IteratorHelper {
        @Serial private static final long serialVersionUID = 1L;

        private final Callable mapper;
        private long counter;

        MapHelper(IteratorRecord underlying, Callable mapper) {
            super(underlying);
            this.mapper = mapper;
        }

        @Override
        Object step(Context cx, VarScope s) {
            Object value = underlying.stepValue(cx, s);
            if (value == DONE) {
                return DONE;
            }
            return callAndCloseOnError(cx, s, underlying, mapper, value, (double) counter++);
        }
    }

    private static final class FilterHelper extends IteratorHelper {
        @Serial private static final long serialVersionUID = 1L;

        private final Callable predicate;
        private long counter;

        FilterHelper(IteratorRecord underlying, Callable predicate) {
            super(underlying);
            this.predicate = predicate;
        }

        @Override
        Object step(Context cx, VarScope s) {
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
        @Serial private static final long serialVersionUID = 1L;

        private double remaining;

        TakeHelper(IteratorRecord underlying, double limit) {
            super(underlying);
            this.remaining = limit;
        }

        @Override
        Object step(Context cx, VarScope s) {
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
        @Serial private static final long serialVersionUID = 1L;

        private double remaining;

        DropHelper(IteratorRecord underlying, double limit) {
            super(underlying);
            this.remaining = limit;
        }

        @Override
        Object step(Context cx, VarScope s) {
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
        @Serial private static final long serialVersionUID = 1L;

        private final Callable mapper;
        private long counter;
        private IteratorRecord inner;

        FlatMapHelper(IteratorRecord underlying, Callable mapper) {
            super(underlying);
            this.mapper = mapper;
        }

        @Override
        Object step(Context cx, VarScope s) {
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
        void closeAfterYield(Context cx, VarScope s) {
            try {
                inner.close(cx, s);
            } catch (RhinoException e) {
                throw closeOnError(cx, s, underlying.iterator, e);
            }
            underlying.close(cx, s);
        }
    }

    private static final class ConcatHelper extends IteratorHelper {
        @Serial private static final long serialVersionUID = 1L;

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
        Object step(Context cx, VarScope s) {
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
        void closeAfterYield(Context cx, VarScope s) {
            current.close(cx, s);
        }
    }
}
