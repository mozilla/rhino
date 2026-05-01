/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.mozilla.javascript.ClassDescriptor.Builder.value;
import static org.mozilla.javascript.ClassDescriptor.Destination.CTOR;
import static org.mozilla.javascript.ClassDescriptor.Destination.PROTO;

public abstract class ES6Iterator extends ScriptableObject {

    private static final long serialVersionUID = 2438373029140003950L;

    public static final String NEXT_METHOD = "next";
    public static final String DONE_PROPERTY = "done";
    public static final String RETURN_PROPERTY = "return";
    public static final String VALUE_PROPERTY = "value";
    public static final String RETURN_METHOD = "return";

    static final String ITERATOR_CLASS_NAME = "Iterator";
    // Distinct from ITERATOR_CLASS_NAME: the "Iterator" associateValue slot is
    // owned by NativeIterator for the StopIteration object (consulted by generators).
    static final String ITERATOR_PROTOTYPE_TAG = "IteratorPrototype";
    static final String WRAP_FOR_VALID_TAG = "IteratorHelper";

    public static ClassDescriptor makeDescriptor(String name, String tag) {
        // Need a way to associate the built object with the tag. We
        // kind of need this in general, and at the top level, but we
        // can bodge it for now.
        return new ClassDescriptor.Builder(name)
                .withMethod(CTOR, "next", 0, ES6Iterator::js_next)
                .withMethod(CTOR, SymbolKey.ITERATOR, 1, ES6Iterator::js_iterator)
                .withProp(CTOR, SymbolKey.TO_STRING_TAG, value(tag, DONTENUM | READONLY))
                .build();
    }

    /**
     * Variant of {@link #makeDescriptor} for iterator <em>helpers</em> that need a {@code return}
     * method on their prototype so consumers can close them (forwarding the close to any wrapped
     * source iterator).
     */
    public static ClassDescriptor makeHelperDescriptor(String name, String tag) {
        return new ClassDescriptor.Builder(name)
                .withMethod(CTOR, "next", 0, ES6Iterator::js_next)
                .withMethod(CTOR, RETURN_METHOD, 0, ES6Iterator::js_returnMethod)
                .withMethod(CTOR, SymbolKey.ITERATOR, 1, ES6Iterator::js_iterator)
                .withProp(CTOR, SymbolKey.TO_STRING_TAG, value(tag, DONTENUM | READONLY))
                .build();
    }

    public static void initialize(
            ClassDescriptor desc,
            Context cx,
            TopLevel scope,
            ScriptableObject obj,
            boolean sealed,
            String name) {
        // Defer sealing until after we have a chance to re-parent the populated
        // prototype under %Iterator.prototype% when it is available.
        var global = desc.populateGlobal(cx, scope, obj, false);
        Object iterProto = ScriptableObject.getTopScopeValue(scope, ITERATOR_PROTOTYPE_TAG);
        if (iterProto instanceof Scriptable) {
            global.setPrototype((Scriptable) iterProto);
        }
        if (sealed) {
            global.sealObject();
        }
        scope.associateValue(name, global);
    }

    private static final ClassDescriptor ITERATOR_CTOR_DESCRIPTOR =
            new ClassDescriptor.Builder(
                            ITERATOR_CLASS_NAME,
                            0,
                            ClassDescriptor.typeError(),
                            ES6Iterator::js_construct)
                    .withMethod(CTOR, "from", 1, ES6Iterator::js_from)
                    .withMethod(PROTO, SymbolKey.ITERATOR, 0, ES6Iterator::js_iterator)
                    .withProp(
                            PROTO,
                            SymbolKey.TO_STRING_TAG,
                            value(ITERATOR_CLASS_NAME, DONTENUM | READONLY))
                    .withMethod(PROTO, "toArray", 0, ES6Iterator::js_toArray)
                    .withMethod(PROTO, "forEach", 1, ES6Iterator::js_forEach)
                    .withMethod(PROTO, "reduce", 1, ES6Iterator::js_reduce)
                    .withMethod(PROTO, "some", 1, ES6Iterator::js_some)
                    .withMethod(PROTO, "every", 1, ES6Iterator::js_every)
                    .withMethod(PROTO, "find", 1, ES6Iterator::js_find)
                    .withMethod(PROTO, "map", 1, ES6Iterator::js_map)
                    .withMethod(PROTO, "filter", 1, ES6Iterator::js_filter)
                    .withMethod(PROTO, "take", 1, ES6Iterator::js_take)
                    .withMethod(PROTO, "drop", 1, ES6Iterator::js_drop)
                    .withMethod(PROTO, "flatMap", 1, ES6Iterator::js_flatMap)
                    .build();

    private static final ClassDescriptor WRAP_FOR_VALID_DESCRIPTOR =
            makeHelperDescriptor(WRAP_FOR_VALID_TAG, "Iterator Helper");

    /**
     * Installs the modern ES2025 {@code Iterator} constructor (with the static {@code from} method)
     * on the given scope, and registers the prototype used by iterators wrapped via {@code
     * Iterator.from}.
     */
    public static void initIteratorConstructor(Context cx, TopLevel scope, boolean sealed) {
        var iteratorProto = new NativeObject();
        ITERATOR_CTOR_DESCRIPTOR.buildConstructor(cx, scope, iteratorProto, sealed);
        // Stash the prototype so later helpers can look it up even if
        // the script replaces the global "Iterator" binding.
        scope.associateValue(ITERATOR_PROTOTYPE_TAG, iteratorProto);

        ES6Iterator.initialize(
                WRAP_FOR_VALID_DESCRIPTOR,
                cx,
                scope,
                new WrapForValidIterator(),
                sealed,
                WRAP_FOR_VALID_TAG);
    }

    protected boolean exhausted = false;
    private String tag;

    protected ES6Iterator() {}

    protected ES6Iterator(VarScope scope, String tag) {
        // Set parent and prototype properties. Since we don't have a
        // "Iterator" constructor in the top scope, we stash the
        // prototype in the top scope's associated value.
        this.tag = tag;
        TopLevel top = ScriptableObject.getTopLevelScope(scope);
        this.setParentScope(top);
        ScriptableObject prototype = (ScriptableObject) ScriptableObject.getTopScopeValue(top, tag);
        setPrototype(prototype);
    }

    private static ES6Iterator realThis(Object thisObj) {
        return LambdaConstructor.convertThisObject(thisObj, ES6Iterator.class);
    }

    private static Object js_next(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        ES6Iterator iterator = realThis(thisObj);
        return iterator.next(cx, s);
    }

    private static Object js_iterator(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return thisObj;
    }

    private static Object js_returnMethod(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        ES6Iterator iterator = realThis(thisObj);
        Object value = args.length > 0 ? args[0] : Undefined.instance;
        return iterator.closeIterator(cx, s, value);
    }

    /**
     * Subclass hook for the JavaScript {@code return} method. The default implementation marks the
     * iterator exhausted and produces {@code {value, done: true}}. Iterator helpers override this
     * to forward the close to their wrapped source iterator.
     */
    protected Object closeIterator(Context cx, VarScope scope, Object value) {
        this.exhausted = true;
        return makeIteratorResult(cx, scope, Boolean.TRUE, value);
    }

    protected abstract boolean isDone(Context cx, VarScope scope);

    protected abstract Object nextValue(Context cx, VarScope scope);

    protected Object next(Context cx, VarScope scope) {
        Object value = Undefined.instance;
        boolean done = isDone(cx, scope) || this.exhausted;
        if (!done) {
            value = nextValue(cx, scope);
        } else {
            this.exhausted = true;
        }
        return makeIteratorResult(cx, scope, Boolean.valueOf(done), value);
    }

    protected String getTag() {
        return tag;
    }

    // 25.1.1.3 The IteratorResult Interface
    static Scriptable makeIteratorResult(Context cx, VarScope scope, Boolean done) {
        return makeIteratorResult(cx, scope, done, Undefined.instance);
    }

    static Scriptable makeIteratorResult(Context cx, VarScope scope, Boolean done, Object value) {
        final Scriptable iteratorResult = cx.newObject(scope);
        ScriptableObject.putProperty(iteratorResult, VALUE_PROPERTY, value);
        ScriptableObject.putProperty(iteratorResult, DONE_PROPERTY, done);
        return iteratorResult;
    }

    /**
     * Implementation of {@code Iterator.from(obj)}. Uses {@link #getIteratorFlattenable} so an
     * {@code ES6Iterator} is returned directly when the argument already represents one; otherwise
     * a {@link WrapForValidIterator} wraps the underlying iterator record.
     */
    private static Object js_from(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        Object arg = args.length == 0 ? Undefined.instance : args[0];
        return getIteratorFlattenable(cx, s, arg);
    }

    /**
     * Spec-equivalent of {@code GetIteratorFlattenable(obj, iterate-string-primitives)} fused with
     * the {@code Iterator.from} step-1 identity check: returns the argument unchanged if it is
     * already an {@code ES6Iterator}, otherwise obtains the underlying iterator (via
     * {@code @@iterator} or directly treating {@code obj} as one) and wraps it.
     */
    public static ES6Iterator getIteratorFlattenable(Context cx, VarScope scope, Object obj) {
        if (obj instanceof ES6Iterator) {
            return (ES6Iterator) obj;
        }
        if (obj instanceof CharSequence) {
            return new NativeStringIterator(scope, obj);
        }
        if (obj == null || Undefined.isUndefined(obj)) {
            throw ScriptRuntime.typeErrorById("msg.not.iterable", ScriptRuntime.toString(obj));
        }
        if (!(obj instanceof Scriptable)) {
            throw ScriptRuntime.typeErrorById("msg.not.iterable", ScriptRuntime.toString(obj));
        }
        Scriptable source = (Scriptable) obj;
        Scriptable iterator;
        if (ScriptableObject.hasProperty(source, SymbolKey.ITERATOR)) {
            Object iteratorFn = ScriptableObject.getProperty(source, SymbolKey.ITERATOR);
            if (Undefined.isUndefined(iteratorFn) || iteratorFn == null) {
                // @@iterator present but nullish: fall through to use source as iterator.
                iterator = source;
            } else {
                if (!(iteratorFn instanceof Callable)) {
                    throw ScriptRuntime.typeErrorById(
                            "msg.not.iterable", ScriptRuntime.toString(obj));
                }
                VarScope callScope =
                        (iteratorFn instanceof Function)
                                ? ((Function) iteratorFn).getDeclarationScope()
                                : cx.topCallScope;
                Object v =
                        ((Callable) iteratorFn)
                                .call(cx, callScope, source, ScriptRuntime.emptyArgs);
                if (!(v instanceof Scriptable)) {
                    throw ScriptRuntime.typeErrorById(
                            "msg.not.iterable", ScriptRuntime.toString(obj));
                }
                iterator = (Scriptable) v;
            }
        } else {
            // Flatten case: treat `obj` itself as the iterator record.
            iterator = source;
        }

        if (iterator instanceof ES6Iterator) {
            return (ES6Iterator) iterator;
        }
        return new WrapForValidIterator(scope, iterator);
    }

    /**
     * Constructor behaviour for the modern {@code Iterator} global. The constructor is not meant to
     * be called directly: {@code new Iterator()} throws a TypeError. When invoked as {@code
     * super()} from a subclass constructor (i.e. NewTarget is distinct from the Iterator
     * constructor itself) a plain object inheriting from the subclass prototype is returned, so
     * {@code class Foo extends Iterator} remains usable.
     */
    private static Object js_construct(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        if (nt == null || nt == f) {
            throw ScriptRuntime.typeErrorById("msg.iterator.abstract");
        }
        NativeObject obj = new NativeObject();
        ScriptRuntime.setBuiltinProtoAndParent(obj, f, nt, s, TopLevel.Builtins.Iterator);
        return obj;
    }

    // ------------------------------------------------------------
    // Eager Iterator-prototype helpers (toArray, forEach, reduce, some, every, find)
    // ------------------------------------------------------------

    private static Scriptable requireIteratorReceiver(Object thisObj) {
        if (!(thisObj instanceof Scriptable) || thisObj == null || Undefined.isUndefined(thisObj)) {
            throw ScriptRuntime.typeErrorById("msg.not.iterable", ScriptRuntime.toString(thisObj));
        }
        return (Scriptable) thisObj;
    }

    private static Callable requireNext(Scriptable iter) {
        Object next = ScriptableObject.getProperty(iter, NEXT_METHOD);
        if (next == Scriptable.NOT_FOUND) {
            next = Undefined.instance;
        }
        if (!(next instanceof Callable)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.isnt.function", NEXT_METHOD, ScriptRuntime.typeof(next));
        }
        return (Callable) next;
    }

    private static Callable requireCallback(Object arg) {
        if (!(arg instanceof Callable)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.isnt.function", ScriptRuntime.toString(arg), ScriptRuntime.typeof(arg));
        }
        return (Callable) arg;
    }

    private static VarScope callScopeFor(Context cx, Callable fn) {
        return (fn instanceof Function) ? ((Function) fn).getDeclarationScope() : cx.topCallScope;
    }

    /** Returns the IteratorResult object, or {@code null} if the iterator is exhausted. */
    private static Scriptable iteratorStep(
            Context cx, VarScope scope, Scriptable iter, Callable nextFn) {
        Object v = nextFn.call(cx, callScopeFor(cx, nextFn), iter, ScriptRuntime.emptyArgs);
        if (!(v instanceof Scriptable)) {
            throw ScriptRuntime.typeError("Iterator result is not an object");
        }
        Scriptable r = (Scriptable) v;
        Object done = ScriptableObject.getProperty(r, DONE_PROPERTY);
        if (ScriptRuntime.toBoolean(done)) {
            return null;
        }
        return r;
    }

    private static Object iteratorValue(Scriptable result) {
        Object v = ScriptableObject.getProperty(result, VALUE_PROPERTY);
        return (v == Scriptable.NOT_FOUND) ? Undefined.instance : v;
    }

    /**
     * Spec {@code IteratorClose} with a normal completion: invokes {@code return} if present;
     * throws from {@code return} propagate to the caller.
     */
    private static void iteratorClose(Context cx, VarScope scope, Scriptable iter) {
        if (!ScriptableObject.hasProperty(iter, RETURN_METHOD)) return;
        Object ret = ScriptableObject.getProperty(iter, RETURN_METHOD);
        if (ret == null || Undefined.isUndefined(ret)) return;
        if (!(ret instanceof Callable)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.isnt.function", RETURN_METHOD, ScriptRuntime.typeof(ret));
        }
        Object result =
                ((Callable) ret)
                        .call(cx, callScopeFor(cx, (Callable) ret), iter, ScriptRuntime.emptyArgs);
        if (!(result instanceof Scriptable)) {
            throw ScriptRuntime.typeError("Iterator return() result is not an object");
        }
    }

    /**
     * Spec {@code IteratorClose} with a throw completion: any error raised by the iterator's {@code
     * return} method is swallowed and the original exception is re-thrown.
     */
    private static RhinoException iteratorCloseOnThrow(
            Context cx, VarScope scope, Scriptable iter, RhinoException original) {
        try {
            if (ScriptableObject.hasProperty(iter, RETURN_METHOD)) {
                Object ret = ScriptableObject.getProperty(iter, RETURN_METHOD);
                if (ret instanceof Callable) {
                    ((Callable) ret)
                            .call(
                                    cx,
                                    callScopeFor(cx, (Callable) ret),
                                    iter,
                                    ScriptRuntime.emptyArgs);
                }
            }
        } catch (RhinoException ignored) {
            // Per spec, the original throw wins; any error from return() is discarded.
        }
        return original;
    }

    private static Object js_toArray(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        Scriptable iter = requireIteratorReceiver(thisObj);
        Callable nextFn = requireNext(iter);
        java.util.ArrayList<Object> buffer = new java.util.ArrayList<>();
        for (; ; ) {
            Scriptable step = iteratorStep(cx, s, iter, nextFn);
            if (step == null) {
                return cx.newArray(s, buffer.toArray());
            }
            buffer.add(iteratorValue(step));
        }
    }

    private static Object js_forEach(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        Scriptable iter = requireIteratorReceiver(thisObj);
        Callable nextFn = requireNext(iter);
        Callable fn = requireCallback(args.length > 0 ? args[0] : Undefined.instance);
        VarScope fnScope = callScopeFor(cx, fn);
        int counter = 0;
        for (; ; ) {
            Scriptable step = iteratorStep(cx, s, iter, nextFn);
            if (step == null) return Undefined.instance;
            Object val = iteratorValue(step);
            try {
                fn.call(
                        cx,
                        fnScope,
                        Undefined.instance,
                        new Object[] {val, Integer.valueOf(counter++)});
            } catch (RhinoException e) {
                throw iteratorCloseOnThrow(cx, s, iter, e);
            }
        }
    }

    private static Object js_reduce(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        Scriptable iter = requireIteratorReceiver(thisObj);
        Callable nextFn = requireNext(iter);
        Callable reducer = requireCallback(args.length > 0 ? args[0] : Undefined.instance);
        VarScope rScope = callScopeFor(cx, reducer);
        boolean hasInitial = args.length >= 2;
        Object accumulator;
        int counter = 0;
        if (hasInitial) {
            accumulator = args[1];
        } else {
            Scriptable firstStep = iteratorStep(cx, s, iter, nextFn);
            if (firstStep == null) {
                throw ScriptRuntime.typeErrorById("msg.empty.array.reduce");
            }
            accumulator = iteratorValue(firstStep);
            counter = 1;
        }
        for (; ; ) {
            Scriptable step = iteratorStep(cx, s, iter, nextFn);
            if (step == null) return accumulator;
            Object val = iteratorValue(step);
            try {
                accumulator =
                        reducer.call(
                                cx,
                                rScope,
                                Undefined.instance,
                                new Object[] {accumulator, val, Integer.valueOf(counter++)});
            } catch (RhinoException e) {
                throw iteratorCloseOnThrow(cx, s, iter, e);
            }
        }
    }

    private static Object js_some(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return js_predicate(cx, s, thisObj, args, PredicateKind.SOME);
    }

    private static Object js_every(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return js_predicate(cx, s, thisObj, args, PredicateKind.EVERY);
    }

    private static Object js_find(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return js_predicate(cx, s, thisObj, args, PredicateKind.FIND);
    }

    private enum PredicateKind {
        SOME,
        EVERY,
        FIND
    }

    private static Object js_predicate(
            Context cx, VarScope scope, Object thisObj, Object[] args, PredicateKind kind) {
        Scriptable iter = requireIteratorReceiver(thisObj);
        Callable nextFn = requireNext(iter);
        Callable pred = requireCallback(args.length > 0 ? args[0] : Undefined.instance);
        VarScope pScope = callScopeFor(cx, pred);
        int counter = 0;
        for (; ; ) {
            Scriptable step = iteratorStep(cx, scope, iter, nextFn);
            if (step == null) {
                return kind == PredicateKind.EVERY
                        ? Boolean.TRUE
                        : (kind == PredicateKind.SOME ? Boolean.FALSE : Undefined.instance);
            }
            Object val = iteratorValue(step);
            boolean match;
            try {
                Object r =
                        pred.call(
                                cx,
                                pScope,
                                Undefined.instance,
                                new Object[] {val, Integer.valueOf(counter++)});
                match = ScriptRuntime.toBoolean(r);
            } catch (RhinoException e) {
                throw iteratorCloseOnThrow(cx, scope, iter, e);
            }
            switch (kind) {
                case SOME:
                    if (match) {
                        iteratorClose(cx, scope, iter);
                        return Boolean.TRUE;
                    }
                    break;
                case EVERY:
                    if (!match) {
                        iteratorClose(cx, scope, iter);
                        return Boolean.FALSE;
                    }
                    break;
                case FIND:
                    if (match) {
                        iteratorClose(cx, scope, iter);
                        return val;
                    }
                    break;
            }
        }
    }

    // ------------------------------------------------------------
    // Lazy Iterator-prototype helpers (map, filter, take, drop, flatMap)
    // ------------------------------------------------------------

    private static Object js_map(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        Scriptable iter = requireIteratorReceiver(thisObj);
        Callable nextFn = requireNext(iter);
        Callable mapper = requireCallback(args.length > 0 ? args[0] : Undefined.instance);
        return new MapIterator(s, iter, nextFn, mapper);
    }

    private static Object js_filter(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        Scriptable iter = requireIteratorReceiver(thisObj);
        Callable nextFn = requireNext(iter);
        Callable pred = requireCallback(args.length > 0 ? args[0] : Undefined.instance);
        return new FilterIterator(s, iter, nextFn, pred);
    }

    private static long helperLimit(Object arg) {
        double n = ScriptRuntime.toInteger(arg);
        if (n < 0 || Double.isNaN(n)) {
            throw ScriptRuntime.rangeErrorById("msg.iterator.helper.limit");
        }
        if (Double.isInfinite(n) || n > Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return (long) n;
    }

    private static Object js_take(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        Scriptable iter = requireIteratorReceiver(thisObj);
        Callable nextFn = requireNext(iter);
        long remaining = helperLimit(args.length > 0 ? args[0] : Undefined.instance);
        return new TakeIterator(s, iter, nextFn, remaining);
    }

    private static Object js_drop(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        Scriptable iter = requireIteratorReceiver(thisObj);
        Callable nextFn = requireNext(iter);
        long toSkip = helperLimit(args.length > 0 ? args[0] : Undefined.instance);
        return new DropIterator(s, iter, nextFn, toSkip);
    }

    private static Object js_flatMap(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        Scriptable iter = requireIteratorReceiver(thisObj);
        Callable nextFn = requireNext(iter);
        Callable mapper = requireCallback(args.length > 0 ? args[0] : Undefined.instance);
        return new FlatMapIterator(s, iter, nextFn, mapper);
    }

    /**
     * Shared base for lazy iterator helpers: holds a source iterator with a cached next method and
     * provides default {@code isDone}/{@code nextValue} overrides (the helpers override {@code
     * nextHelper} for their per-step logic, since it doesn't match the isDone/nextValue split).
     *
     * <p>Per spec, an iterator helper's {@code next} runs as a generator closure, so re-entering
     * it while it is already running throws a TypeError. We enforce that with an {@code active}
     * flag set across each {@code next} invocation.
     */
    abstract static class AbstractIteratorHelper extends ES6Iterator {

        private static final long serialVersionUID = 1L;

        protected final Scriptable source;
        protected final Callable sourceNext;
        private boolean active;

        protected AbstractIteratorHelper(VarScope scope, Scriptable source, Callable sourceNext) {
            super(scope, WRAP_FOR_VALID_TAG);
            this.source = source;
            this.sourceNext = sourceNext;
        }

        @Override
        public String getClassName() {
            return "Iterator Helper";
        }

        @Override
        protected boolean isDone(Context cx, VarScope scope) {
            return true;
        }

        @Override
        protected Object nextValue(Context cx, VarScope scope) {
            return Undefined.instance;
        }

        @Override
        protected final Object next(Context cx, VarScope scope) {
            if (active) {
                throw ScriptRuntime.typeErrorById("msg.iterator.helper.executing");
            }
            active = true;
            try {
                return nextHelper(cx, scope);
            } finally {
                active = false;
            }
        }

        protected abstract Object nextHelper(Context cx, VarScope scope);

        @Override
        protected Object closeIterator(Context cx, VarScope scope, Object value) {
            exhausted = true;
            if (source != null) {
                iteratorClose(cx, scope, source);
            }
            return makeIteratorResult(cx, scope, Boolean.TRUE, value);
        }
    }

    static final class MapIterator extends AbstractIteratorHelper {

        private static final long serialVersionUID = 1L;

        private final Callable mapper;
        private int counter = 0;

        MapIterator(VarScope scope, Scriptable source, Callable sourceNext, Callable mapper) {
            super(scope, source, sourceNext);
            this.mapper = mapper;
        }

        @Override
        protected Object nextHelper(Context cx, VarScope scope) {
            if (exhausted) return makeIteratorResult(cx, scope, Boolean.TRUE);
            Scriptable step = iteratorStep(cx, scope, source, sourceNext);
            if (step == null) {
                exhausted = true;
                return makeIteratorResult(cx, scope, Boolean.TRUE);
            }
            Object val = iteratorValue(step);
            Object mapped;
            try {
                mapped =
                        mapper.call(
                                cx,
                                callScopeFor(cx, mapper),
                                Undefined.instance,
                                new Object[] {val, Integer.valueOf(counter++)});
            } catch (RhinoException e) {
                exhausted = true;
                throw iteratorCloseOnThrow(cx, scope, source, e);
            }
            return makeIteratorResult(cx, scope, Boolean.FALSE, mapped);
        }
    }

    static final class FilterIterator extends AbstractIteratorHelper {

        private static final long serialVersionUID = 1L;

        private final Callable predicate;
        private int counter = 0;

        FilterIterator(VarScope scope, Scriptable source, Callable sourceNext, Callable predicate) {
            super(scope, source, sourceNext);
            this.predicate = predicate;
        }

        @Override
        protected Object nextHelper(Context cx, VarScope scope) {
            if (exhausted) return makeIteratorResult(cx, scope, Boolean.TRUE);
            VarScope predScope = callScopeFor(cx, predicate);
            for (; ; ) {
                Scriptable step = iteratorStep(cx, scope, source, sourceNext);
                if (step == null) {
                    exhausted = true;
                    return makeIteratorResult(cx, scope, Boolean.TRUE);
                }
                Object val = iteratorValue(step);
                boolean keep;
                try {
                    Object r =
                            predicate.call(
                                    cx,
                                    predScope,
                                    Undefined.instance,
                                    new Object[] {val, Integer.valueOf(counter++)});
                    keep = ScriptRuntime.toBoolean(r);
                } catch (RhinoException e) {
                    exhausted = true;
                    throw iteratorCloseOnThrow(cx, scope, source, e);
                }
                if (keep) {
                    return makeIteratorResult(cx, scope, Boolean.FALSE, val);
                }
            }
        }
    }

    static final class TakeIterator extends AbstractIteratorHelper {

        private static final long serialVersionUID = 1L;

        private long remaining;

        TakeIterator(VarScope scope, Scriptable source, Callable sourceNext, long remaining) {
            super(scope, source, sourceNext);
            this.remaining = remaining;
        }

        @Override
        protected Object nextHelper(Context cx, VarScope scope) {
            if (exhausted) return makeIteratorResult(cx, scope, Boolean.TRUE);
            if (remaining <= 0) {
                exhausted = true;
                iteratorClose(cx, scope, source);
                return makeIteratorResult(cx, scope, Boolean.TRUE);
            }
            remaining--;
            Scriptable step = iteratorStep(cx, scope, source, sourceNext);
            if (step == null) {
                exhausted = true;
                return makeIteratorResult(cx, scope, Boolean.TRUE);
            }
            return makeIteratorResult(cx, scope, Boolean.FALSE, iteratorValue(step));
        }
    }

    static final class DropIterator extends AbstractIteratorHelper {

        private static final long serialVersionUID = 1L;

        private long toSkip;
        private boolean primed = false;

        DropIterator(VarScope scope, Scriptable source, Callable sourceNext, long toSkip) {
            super(scope, source, sourceNext);
            this.toSkip = toSkip;
        }

        @Override
        protected Object nextHelper(Context cx, VarScope scope) {
            if (exhausted) return makeIteratorResult(cx, scope, Boolean.TRUE);
            if (!primed) {
                primed = true;
                while (toSkip > 0) {
                    toSkip--;
                    Scriptable step = iteratorStep(cx, scope, source, sourceNext);
                    if (step == null) {
                        exhausted = true;
                        return makeIteratorResult(cx, scope, Boolean.TRUE);
                    }
                }
            }
            Scriptable step = iteratorStep(cx, scope, source, sourceNext);
            if (step == null) {
                exhausted = true;
                return makeIteratorResult(cx, scope, Boolean.TRUE);
            }
            return makeIteratorResult(cx, scope, Boolean.FALSE, iteratorValue(step));
        }
    }

    static final class FlatMapIterator extends AbstractIteratorHelper {

        private static final long serialVersionUID = 1L;

        private final Callable mapper;
        private int counter = 0;
        private ES6Iterator inner;

        FlatMapIterator(VarScope scope, Scriptable source, Callable sourceNext, Callable mapper) {
            super(scope, source, sourceNext);
            this.mapper = mapper;
        }

        @Override
        protected Object nextHelper(Context cx, VarScope scope) {
            if (exhausted) return makeIteratorResult(cx, scope, Boolean.TRUE);
            for (; ; ) {
                if (inner != null) {
                    Object innerResult = inner.next(cx, scope);
                    if (!(innerResult instanceof Scriptable)) {
                        exhausted = true;
                        throw ScriptRuntime.typeError("Iterator result is not an object");
                    }
                    Scriptable r = (Scriptable) innerResult;
                    Object done = ScriptableObject.getProperty(r, DONE_PROPERTY);
                    if (!ScriptRuntime.toBoolean(done)) {
                        return r;
                    }
                    inner = null;
                }
                Scriptable step = iteratorStep(cx, scope, source, sourceNext);
                if (step == null) {
                    exhausted = true;
                    return makeIteratorResult(cx, scope, Boolean.TRUE);
                }
                Object val = iteratorValue(step);
                Object mapped;
                try {
                    mapped =
                            mapper.call(
                                    cx,
                                    callScopeFor(cx, mapper),
                                    Undefined.instance,
                                    new Object[] {val, Integer.valueOf(counter++)});
                } catch (RhinoException e) {
                    exhausted = true;
                    throw iteratorCloseOnThrow(cx, scope, source, e);
                }
                try {
                    inner = getIteratorFlattenable(cx, scope, mapped);
                } catch (RhinoException e) {
                    exhausted = true;
                    throw iteratorCloseOnThrow(cx, scope, source, e);
                }
            }
        }

        @Override
        protected Object closeIterator(Context cx, VarScope scope, Object value) {
            exhausted = true;
            RhinoException pending = null;
            if (inner != null) {
                try {
                    inner.closeIterator(cx, scope, Undefined.instance);
                } catch (RhinoException e) {
                    pending = e;
                }
                inner = null;
            }
            try {
                iteratorClose(cx, scope, source);
            } catch (RhinoException e) {
                if (pending == null) pending = e;
            }
            if (pending != null) throw pending;
            return makeIteratorResult(cx, scope, Boolean.TRUE, value);
        }
    }

    /**
     * Wraps an arbitrary iterator record (an iterator object plus its {@code next} method) as an
     * {@link ES6Iterator}. The wrapped iterator is consulted directly for each step; the
     * IteratorResult object produced by the inner {@code next} is returned unchanged.
     */
    static final class WrapForValidIterator extends ES6Iterator {

        private static final long serialVersionUID = 1L;

        private Scriptable wrapped;
        private Callable wrappedNext;

        /** Only used to build the prototype object during initialisation. */
        private WrapForValidIterator() {
            super();
        }

        WrapForValidIterator(VarScope scope, Scriptable wrapped) {
            super(scope, WRAP_FOR_VALID_TAG);
            this.wrapped = wrapped;
            Object nextMethod = ScriptableObject.getProperty(wrapped, NEXT_METHOD);
            if (!(nextMethod instanceof Callable)) {
                // The iterator is unusable; defer the TypeError until next() is actually
                // invoked, matching spec semantics where GetIteratorDirect only reads next.
                this.wrappedNext = null;
            } else {
                this.wrappedNext = (Callable) nextMethod;
            }
        }

        @Override
        public String getClassName() {
            return "Iterator Helper";
        }

        @Override
        protected boolean isDone(Context cx, VarScope scope) {
            return true;
        }

        @Override
        protected Object nextValue(Context cx, VarScope scope) {
            return Undefined.instance;
        }

        @Override
        protected Object next(Context cx, VarScope scope) {
            if (exhausted) {
                return makeIteratorResult(cx, scope, Boolean.TRUE);
            }
            if (wrappedNext == null) {
                exhausted = true;
                throw ScriptRuntime.typeErrorById(
                        "msg.not.iterable", ScriptRuntime.toString(wrapped));
            }
            Object result =
                    wrappedNext.call(
                            cx, callScopeFor(cx, wrappedNext), wrapped, ScriptRuntime.emptyArgs);
            if (!(result instanceof Scriptable)) {
                exhausted = true;
                throw ScriptRuntime.typeErrorById(
                        "msg.not.iterable", ScriptRuntime.toString(wrapped));
            }
            Scriptable resultObj = (Scriptable) result;
            Object done = ScriptableObject.getProperty(resultObj, DONE_PROPERTY);
            if (ScriptRuntime.toBoolean(done)) {
                exhausted = true;
            }
            return resultObj;
        }

        @Override
        protected Object closeIterator(Context cx, VarScope scope, Object value) {
            exhausted = true;
            if (wrapped != null) {
                iteratorClose(cx, scope, wrapped);
            }
            return makeIteratorResult(cx, scope, Boolean.TRUE, value);
        }
    }
}
