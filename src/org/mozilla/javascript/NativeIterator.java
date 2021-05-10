/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * This class implements iterator objects. See
 * http://developer.mozilla.org/en/docs/New_in_JavaScript_1.7#Iterators
 *
 * @author Norris Boyd
 */
public final class NativeIterator extends IdScriptableObject {
    private static final long serialVersionUID = -4136968203581667681L;
    private static final Object ITERATOR_TAG = "Iterator";

    // Functions are registered as '__iterator__' for Iterables and Maps 
    public static final BaseFunction JAVA_COLLECTION_ITERATOR = new CollectionIteratorFunction();
    public static final BaseFunction JAVA_MAP_ITERATOR = new MapIteratorFunction();

    static void init(Context cx, ScriptableObject scope, boolean sealed) {
        // Iterator
        NativeIterator iterator = new NativeIterator();
        iterator.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);

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
        if (sealed) { obj.sealObject(); }
        ScriptableObject.defineProperty(scope, STOP_ITERATION, obj,
                                        ScriptableObject.DONTENUM);
        // Use "associateValue" so that generators can continue to
        // throw StopIteration even if the property of the global
        // scope is replaced or deleted.
        scope.associateValue(ITERATOR_TAG, obj);
    }

    /**
     * Only for constructing the prototype object.
     */
    private NativeIterator() {
    }

    private NativeIterator(Object objectIterator) {
      this.objectIterator = objectIterator;
    }

    /**
     * Get the value of the "StopIteration" object. Note that this value
     * is stored in the top-level scope using "associateValue" so the
     * value can still be found even if a script overwrites or deletes
     * the global "StopIteration" property.
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
        return "Iterator";
    }

    @Override
    protected void initPrototypeId(int id) {
        String s;
        int arity;
        switch (id) {
          case Id_constructor:    arity=2; s="constructor";          break;
          case Id_next:           arity=0; s="next";                 break;
          case Id___iterator__:   arity=1; s=ITERATOR_PROPERTY_NAME; break;
          default: throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(ITERATOR_TAG, id, s, arity);
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Scriptable thisObj, Object[] args)
    {
        if (!f.hasTag(ITERATOR_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();

        if (id == Id_constructor) {
            return jsConstructor(cx, scope, thisObj, args);
        }

        NativeIterator iterator = ensureType(thisObj, NativeIterator.class, f);

        switch (id) {

          case Id_next:
            return iterator.next(cx, scope);

          case Id___iterator__:
            /// XXX: what about argument? SpiderMonkey apparently ignores it
            return thisObj;

          default:
            throw new IllegalArgumentException(String.valueOf(id));
        }
    }

    /* The JavaScript constructor */
    private static Object jsConstructor(Context cx, Scriptable scope,
                                        Scriptable thisObj, Object[] args)
    {
        if (args.length == 0 || args[0] == null ||
            args[0] == Undefined.instance)
        {
            Object argument = args.length == 0 ? Undefined.instance : args[0];
            throw ScriptRuntime.typeErrorById("msg.no.properties",
                                           ScriptRuntime.toString(argument));
        }
        Scriptable obj = ScriptRuntime.toObject(cx, scope, args[0]);
        boolean keyOnly = args.length > 1 && ScriptRuntime.toBoolean(args[1]);
        if (thisObj != null) {
            // Called as a function. Convert to iterator if possible.

            // For objects that implement java.lang.Iterable or
            // java.util.Iterator, have JavaScript Iterator call the underlying
            // iteration methods
            Iterator<?> iterator = getJavaIterator(obj);
            if (iterator != null) {
                scope = ScriptableObject.getTopLevelScope(scope);
                return cx.getWrapFactory().wrap(cx, scope,
                        new WrappedJavaIterator(iterator, scope),
                        WrappedJavaIterator.class);
            }

            // Otherwise, just call the runtime routine
            Scriptable jsIterator = ScriptRuntime.toIterator(cx, scope, obj,
                                                             keyOnly);
            if (jsIterator != null) {
                return jsIterator;
            }
        }

        // Otherwise, just set up to iterate over the properties of the object.
        // Do not call __iterator__ method.
        Object objectIterator = ScriptRuntime.enumInit(obj, cx, scope,
            keyOnly ? ScriptRuntime.ENUMERATE_KEYS_NO_ITERATOR
                    : ScriptRuntime.ENUMERATE_ARRAY_NO_ITERATOR);
        ScriptRuntime.setEnumNumbers(objectIterator, true);
        NativeIterator result = new NativeIterator(objectIterator);
        result.setPrototype(ScriptableObject.getClassPrototype(scope,
                                result.getClassName()));
        result.setParentScope(scope);
        return result;
    }

    private Object next(Context cx, Scriptable scope) {
        Boolean b = ScriptRuntime.enumNext(this.objectIterator);
        if (!b.booleanValue()) {
            // Out of values. Throw StopIteration.
            throw new JavaScriptException(
                NativeIterator.getStopIterationObject(scope), null, 0);
        }
        return ScriptRuntime.enumId(this.objectIterator, cx);
    }

    /**
     * If "obj" is a java.util.Iterator or a java.lang.Iterable, return a
     * wrapping as a JavaScript Iterator. Otherwise, return null.
     * This method is in VMBridge since Iterable is a JDK 1.5 addition.
     */
    static private Iterator<?> getJavaIterator(Object obj) {
        if (obj instanceof Wrapper) {
            Object unwrapped = ((Wrapper) obj).unwrap();
            Iterator<?> iterator = null;
            if (unwrapped instanceof Iterator)
                iterator = (Iterator<?>) unwrapped;
            if (unwrapped instanceof Iterable)
                iterator = ((Iterable<?>)unwrapped).iterator();
            return iterator;
        }
        return null;
    }

    static class CollectionIteratorFunction extends BaseFunction {
        @Override
        public Object call(Context cx, Scriptable scope, Scriptable thisObj,
                Object[] args) {

            Object wrapped = ((NativeJavaObject) thisObj).javaObject;
            if (Boolean.TRUE.equals(args[0])) {
                // key only iterator, we will return an iterator
                // for the sequence of the collection length.
                int length = ((Collection<?>) wrapped).size();
                return cx.getWrapFactory().wrap(cx, scope,
                        new SequenceIterator(length, scope),
                        WrappedJavaIterator.class);
            } else {
                Iterator<?> iter = ((Iterable<?>) wrapped).iterator();
                return cx.getWrapFactory().wrap(cx, scope,
                        new WrappedJavaIterator(iter, scope),
                        WrappedJavaIterator.class);
            }
        }
    }
    
    static public class SequenceIterator
    {
        SequenceIterator(int size, Scriptable scope) {
            this.size = size;
            this.scope = scope;
        }

        public Object next() {
            if (pos >= size) {
                // Out of values. Throw StopIteration.
                throw new JavaScriptException(
                    NativeIterator.getStopIterationObject(scope), null, 0);
            }
            return pos++;
        }

        public Object __iterator__(boolean b) {
            return this;
        }

        private int size;
        private int pos;
        private Scriptable scope;
    }
    
    static class MapIteratorFunction extends BaseFunction {
        @Override
        public Object call(Context cx, Scriptable scope, Scriptable thisObj,
                Object[] args) {

            Map<?, ?> map = (Map<?, ?>) ((NativeJavaObject) thisObj).javaObject;
            Iterator<?> iter;
            if (Boolean.TRUE.equals(args[0])) {
                iter = map.keySet().iterator();
            } else {
                iter = map.values().iterator();
            }
            return cx.getWrapFactory().wrap(cx, scope,
                    new WrappedJavaIterator(iter, scope),
                    WrappedJavaIterator.class);
        }
    }
    
    static public class WrappedJavaIterator
    {
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

        private Iterator<?> iterator;
        private Scriptable scope;
    }

// #string_id_map#

    @Override
    protected int findPrototypeId(String s) {
        int id;
// #generated# Last update: 2021-03-21 09:51:20 MEZ
        switch (s) {
        case "constructor":
            id = Id_constructor;
            break;
        case "next":
            id = Id_next;
            break;
        case "__iterator__":
            id = Id___iterator__;
            break;
        default:
            id = 0;
            break;
        }
// #/generated#
        return id;
    }

    private static final int
        Id_constructor           = 1,
        Id_next                  = 2,
        Id___iterator__          = 3,
        MAX_PROTOTYPE_ID         = 3;

// #/string_id_map#

    private Object objectIterator;
}

