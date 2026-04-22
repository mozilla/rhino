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

    public static void initialize(
            ClassDescriptor desc,
            Context cx,
            TopLevel scope,
            ScriptableObject obj,
            boolean sealed,
            String name) {
        var global = desc.populateGlobal(cx, scope, obj, sealed);
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
                    .build();

    private static final ClassDescriptor WRAP_FOR_VALID_DESCRIPTOR =
            makeDescriptor(WRAP_FOR_VALID_TAG, "Iterator Helper");

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
        obj.setParentScope(s);
        if (nt instanceof Scriptable) {
            Object proto = ScriptableObject.getProperty((Scriptable) nt, "prototype");
            if (proto instanceof Scriptable) {
                obj.setPrototype((Scriptable) proto);
            }
        }
        return obj;
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
            VarScope callScope =
                    (wrappedNext instanceof Function)
                            ? ((Function) wrappedNext).getDeclarationScope()
                            : cx.topCallScope;
            Object result = wrappedNext.call(cx, callScope, wrapped, ScriptRuntime.emptyArgs);
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
    }
}
