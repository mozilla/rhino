/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

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

    static void init(Context cx, TopLevel scope, boolean sealed) {
        LambdaConstructor constructor =
                new LambdaConstructor(
                        scope,
                        CLASS_NAME,
                        2,
                        NativeIterator::jsConstructorCall,
                        NativeIterator::jsConstructor);
        constructor.setPrototypePropertyAttributes(PERMANENT | READONLY | DONTENUM);

        NativeIterator proto = new NativeIterator();
        constructor.setPrototypeScriptable(proto);

        constructor.definePrototypeMethod(scope, "next", 0, NativeIterator::js_next);
        constructor.definePrototypeMethod(
                scope, ITERATOR_PROPERTY_NAME, 1, NativeIterator::js_iteratorMethod);

        ScriptableObject.defineProperty(scope, CLASS_NAME, constructor, ScriptableObject.DONTENUM);
        if (sealed) {
            constructor.sealObject();
            ((ScriptableObject) constructor.getPrototypeProperty()).sealObject();
        }

        // Generator
        if (cx.getLanguageVersion() >= Context.VERSION_ES6) {
            ES6Generator.init(scope, sealed);
        } else {
            NativeGenerator.init(scope, sealed);
        }

        // StopIteration
        NativeObject obj = new StopIteration();
        obj.setPrototype(getObjectPrototype(scope));
        obj.setParentScope(scope);
        if (sealed) {
            obj.sealObject();
        }
        ScriptableObject.defineProperty(scope, STOP_ITERATION, obj, ScriptableObject.DONTENUM);
        // Use "associateValue" so that generators can continue to
        // throw StopIteration even if the property of the global
        // scope is replaced or deleted.
        scope.associateValue(ITERATOR_TAG, obj);
        scope.getGlobalThis().associateValue(ITERATOR_TAG, obj);
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
    public static Object getStopIterationObject(Scriptable scope) {
        Scriptable top = ScriptableObject.getTopLevelScope(scope);
        return ScriptableObject.getTopScopeValue(top, ITERATOR_TAG);
    }

    private static final String STOP_ITERATION = "StopIteration";
    public static final String ITERATOR_PROPERTY_NAME = "__iterator__";

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
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Scriptable target = requireIteratorTarget(cx, scope, args);
        boolean keyOnly = isKeyOnly(args);

        Iterator<?> iterator = getJavaIterator(target);
        if (iterator != null) {
            Scriptable topScope = ScriptableObject.getTopLevelScope(scope);
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

        return createNativeIterator(cx, scope, target, keyOnly);
    }

    private static Scriptable jsConstructor(Context cx, Scriptable scope, Object[] args) {
        Scriptable target = requireIteratorTarget(cx, scope, args);
        boolean keyOnly = isKeyOnly(args);
        return createNativeIterator(cx, scope, target, keyOnly);
    }

    private static Scriptable requireIteratorTarget(Context cx, Scriptable scope, Object[] args) {
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

    private static Object js_next(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeIterator iterator = realThis(thisObj);
        return iterator.next(cx, scope);
    }

    private static NativeIterator createNativeIterator(
            Context cx, Scriptable scope, Scriptable obj, boolean keyOnly) {
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
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return realThis(thisObj);
    }

    private static NativeIterator realThis(Scriptable thisObj) {
        return LambdaConstructor.convertThisObject(thisObj, NativeIterator.class);
    }

    private Object next(Context cx, Scriptable scope) {
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
        private final Scriptable scope;

        WrappedJavaIterator(Iterator<?> iterator, Scriptable scope) {
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
