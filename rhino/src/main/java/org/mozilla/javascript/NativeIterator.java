/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import static org.mozilla.javascript.ClassDescriptor.Destination.PROTO;

import java.util.Iterator;

/**
 * This class implements iterator objects. See <a
 * href="http://developer.mozilla.org/en/docs/New_in_JavaScript_1.7#Iterators">Iterators</a>
 *
 * @author Norris Boyd
 */
public final class NativeIterator extends ScriptableObject {
    private static final long serialVersionUID = -4136968203581667681L;
    private static final Object ITERATOR_TAG = "Iterator";
    private static final String CLASS_NAME = "Iterator";

    private Object objectIterator;

    private static final String STOP_ITERATION = "StopIteration";
    public static final String ITERATOR_PROPERTY_NAME = "__iterator__";

    private static final ClassDescriptor DESCRIPTOR;

    private static final ClassDescriptor STOP_ITER_DESCRIPTOR;

    static {
        DESCRIPTOR =
                new ClassDescriptor.Builder(
                                CLASS_NAME,
                                2,
                                NativeIterator::jsConstructorCall,
                                NativeIterator::jsConstructor)
                        .withMethod(PROTO, "next", 0, NativeIterator::js_next)
                        .withMethod(
                                PROTO, ITERATOR_PROPERTY_NAME, 1, NativeIterator::js_iteratorMethod)
                        .build();
        STOP_ITER_DESCRIPTOR = new ClassDescriptor.Builder(STOP_ITERATION, 0, null, null).build();
    }

    static void init(Context cx, TopLevel scope, boolean sealed) {
        NativeIterator proto = new NativeIterator();
        var ctor = DESCRIPTOR.buildConstructor(cx, scope, proto, sealed);

        // Generator
        if (cx.getLanguageVersion() >= Context.VERSION_ES6) {
            ES6Generator.init(scope, sealed);
        } else {
            NativeGenerator.init(scope, sealed);
        }

        // StopIteration
        var stopCtor = STOP_ITER_DESCRIPTOR.populateGlobal(cx, scope, new StopIteration(), sealed);
        // Use "associateValue" so that generators can continue to
        // throw StopIteration even if the property of the global
        // scope is replaced or deleted.
        scope.associateValue(ITERATOR_TAG, stopCtor);
        scope.getGlobalThis().associateValue(ITERATOR_TAG, stopCtor);
    }

    /** Only for constructing the prototype object. */
    private NativeIterator() {}

    private NativeIterator(Object objectIterator) {
        this.objectIterator = objectIterator;
    }

    /**
     * Get the value of the "StopIteration" object. Note that this value is stored in the top-level
     * scope using "associateValue" so the value can still be found even if a script overwrites or
     * deletes the global "StopIteration" property.
     *
     * @param scope a scope whose parent chain reaches a top-level scope
     * @return the StopIteration object
     */
    public static Object getStopIterationObject(VarScope scope) {
        TopLevel top = ScriptableObject.getTopLevelScope(scope);
        return ScriptableObject.getTopScopeValue(top, ITERATOR_TAG);
    }

    public static class StopIteration extends NativeObject {
        private static final long serialVersionUID = 2485151085722377663L;

        private Object value = Undefined.instance;

        public StopIteration() {}

        public StopIteration(Object val) {
            this.value = val;
        }

        public Object getValue() {
            return value;
        }

        @Override
        public String getClassName() {
            return STOP_ITERATION;
        }

        /* StopIteration has custom instanceof behavior since it
         * doesn't have a constructor.
         */
        @Override
        public boolean hasInstance(Scriptable instance) {
            return instance instanceof StopIteration;
        }
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    private static Object jsConstructorCall(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        Scriptable target = requireIteratorTarget(cx, s, args);
        boolean keyOnly = isKeyOnly(args);

        Iterator<?> iterator = getJavaIterator(target);
        if (iterator != null) {
            VarScope topScope = ScriptableObject.getTopLevelScope(s);
            return cx.getWrapFactory()
                    .wrap(
                            cx,
                            topScope,
                            new WrappedJavaIterator(iterator, topScope),
                            WrappedJavaIterator.class);
        }

        Scriptable jsIterator = ScriptRuntime.toIterator(cx, target, keyOnly);
        if (jsIterator != null) {
            return jsIterator;
        }

        return createNativeIterator(cx, s, target, keyOnly);
    }

    private static Scriptable jsConstructor(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        Scriptable target = requireIteratorTarget(cx, s, args);
        boolean keyOnly = isKeyOnly(args);
        return createNativeIterator(cx, s, target, keyOnly);
    }

    private static Scriptable requireIteratorTarget(Context cx, VarScope scope, Object[] args) {
        if (args.length == 0 || args[0] == null || args[0] == Undefined.instance) {
            Object argument = args.length == 0 ? Undefined.instance : args[0];
            throw ScriptRuntime.typeErrorById(
                    "msg.no.properties", ScriptRuntime.toString(argument));
        }
        return ScriptRuntime.toObject(cx, scope, args[0]);
    }

    private static boolean isKeyOnly(Object[] args) {
        return args.length > 1 && ScriptRuntime.toBoolean(args[1]);
    }

    private static Object js_next(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeIterator iterator = realThis(thisObj);
        return iterator.next(cx, s);
    }

    private static NativeIterator createNativeIterator(
            Context cx, VarScope scope, Scriptable obj, boolean keyOnly) {
        Object objectIterator =
                ScriptRuntime.enumInit(
                        obj,
                        cx,
                        scope,
                        keyOnly
                                ? ScriptRuntime.ENUMERATE_KEYS_NO_ITERATOR
                                : ScriptRuntime.ENUMERATE_ARRAY_NO_ITERATOR);
        ScriptRuntime.setEnumNumbers(objectIterator, true);
        NativeIterator result = new NativeIterator(objectIterator);
        result.setPrototype(ScriptableObject.getClassPrototype(scope, CLASS_NAME));
        result.setParentScope(scope);
        return result;
    }

    private static Object js_iteratorMethod(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return realThis(thisObj);
    }

    private static NativeIterator realThis(Object thisObj) {
        return LambdaConstructor.convertThisObject(thisObj, NativeIterator.class);
    }

    private Object next(Context cx, VarScope scope) {
        Boolean b = ScriptRuntime.enumNext(this.objectIterator, cx);
        if (!b) {
            // Out of values. Throw StopIteration.
            throw new JavaScriptException(NativeIterator.getStopIterationObject(scope), null, 0);
        }
        return ScriptRuntime.enumId(this.objectIterator, cx);
    }

    /**
     * If "obj" is a java.util.Iterator or a java.lang.Iterable, return a wrapping as a JavaScript
     * Iterator. Otherwise, return null.
     */
    private static Iterator<?> getJavaIterator(Object obj) {
        if (obj instanceof Wrapper) {
            Object unwrapped = ((Wrapper) obj).unwrap();
            Iterator<?> iterator = null;
            if (unwrapped instanceof Iterator) iterator = (Iterator<?>) unwrapped;
            if (unwrapped instanceof Iterable) iterator = ((Iterable<?>) unwrapped).iterator();
            return iterator;
        }
        return null;
    }

    public static class WrappedJavaIterator {
        private final Iterator<?> iterator;
        private final VarScope scope;

        WrappedJavaIterator(Iterator<?> iterator, VarScope scope) {
            this.iterator = iterator;
            this.scope = scope;
        }

        public Object next() {
            if (!iterator.hasNext()) {
                // Out of values. Throw StopIteration.
                throw new JavaScriptException(
                        NativeIterator.getStopIterationObject(scope), null, 0);
            }
            return iterator.next();
        }

        public Object __iterator__(boolean b) {
            return this;
        }
    }
}
