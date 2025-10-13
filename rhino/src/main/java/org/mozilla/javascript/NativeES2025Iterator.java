/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * Implementation of the ES2025 Iterator constructor and Iterator.prototype. This provides the
 * Iterator constructor function and static methods like Iterator.from.
 */
public class NativeES2025Iterator extends IdScriptableObject {
    private static final long serialVersionUID = 1L;

    private static final String ITERATOR_TAG = "Iterator";

    // Constructor IDs
    private static final int Id_constructor = 1;

    // Static method IDs
    private static final int Id_from = 2;
    private static final int Id_concat = 3;

    // Prototype method IDs
    private static final int Id_next = 4;
    private static final int Id_return = 5;
    private static final int Id_throw = 6;

    // Prototype helper method IDs (ES2025)
    private static final int Id_map = 7;
    private static final int Id_filter = 8;
    private static final int Id_take = 9;
    private static final int Id_drop = 10;
    private static final int Id_flatMap = 11;
    private static final int Id_reduce = 12;
    private static final int Id_toArray = 13;
    private static final int Id_forEach = 14;
    private static final int Id_some = 15;
    private static final int Id_every = 16;
    private static final int Id_find = 17;

    // Symbol IDs
    private static final int SymbolId_iterator = 18;
    private static final int SymbolId_toStringTag = 19;

    private static final int MAX_PROTOTYPE_ID = SymbolId_toStringTag;

    static void init(Context cx, ScriptableObject scope, boolean sealed) {
        NativeES2025Iterator proto = new NativeES2025Iterator();
        proto.activatePrototypeMap(MAX_PROTOTYPE_ID);
        proto.setPrototype(getObjectPrototype(scope));
        proto.setParentScope(scope);

        NativeES2025Iterator ctor = new NativeES2025Iterator();
        ctor.setPrototype(proto);

        // Set up constructor
        proto.defineProperty("constructor", ctor, DONTENUM);

        // Define static methods on constructor
        ctor.defineProperty(
                "from",
                new IdFunctionObject(ctor, ITERATOR_TAG, Id_from, "from", 1, scope),
                DONTENUM);
        ctor.defineProperty(
                "concat",
                new IdFunctionObject(ctor, ITERATOR_TAG, Id_concat, "concat", 0, scope),
                DONTENUM);

        // Add constructor to global scope
        ScriptableObject.defineProperty(scope, ITERATOR_TAG, ctor, DONTENUM);

        if (sealed) {
            proto.sealObject();
            ctor.sealObject();
        }
    }

    private NativeES2025Iterator() {}

    @Override
    public String getClassName() {
        return ITERATOR_TAG;
    }

    @Override
    protected void initPrototypeId(int id) {
        if (id <= MAX_PROTOTYPE_ID) {
            String name;
            int arity;
            switch (id) {
                case Id_constructor:
                    name = "constructor";
                    arity = 0;
                    break;
                case Id_next:
                    name = "next";
                    arity = 0;
                    break;
                case Id_return:
                    name = "return";
                    arity = 1;
                    break;
                case Id_throw:
                    name = "throw";
                    arity = 1;
                    break;
                case Id_map:
                    name = "map";
                    arity = 1;
                    break;
                case Id_filter:
                    name = "filter";
                    arity = 1;
                    break;
                case Id_take:
                    name = "take";
                    arity = 1;
                    break;
                case Id_drop:
                    name = "drop";
                    arity = 1;
                    break;
                case Id_flatMap:
                    name = "flatMap";
                    arity = 1;
                    break;
                case Id_reduce:
                    name = "reduce";
                    arity = 1;
                    break;
                case Id_toArray:
                    name = "toArray";
                    arity = 0;
                    break;
                case Id_forEach:
                    name = "forEach";
                    arity = 1;
                    break;
                case Id_some:
                    name = "some";
                    arity = 1;
                    break;
                case Id_every:
                    name = "every";
                    arity = 1;
                    break;
                case Id_find:
                    name = "find";
                    arity = 1;
                    break;
                case SymbolId_iterator:
                    initPrototypeMethod(
                            ITERATOR_TAG, id, SymbolKey.ITERATOR, "[Symbol.iterator]", 0);
                    return;
                case SymbolId_toStringTag:
                    initPrototypeValue(
                            SymbolId_toStringTag,
                            SymbolKey.TO_STRING_TAG,
                            ITERATOR_TAG,
                            DONTENUM | READONLY);
                    return;
                default:
                    throw new IllegalStateException(String.valueOf(id));
            }
            initPrototypeMethod(ITERATOR_TAG, id, name, arity);
        }
    }

    @Override
    public Object execIdCall(
            IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(ITERATOR_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }

        int id = f.methodId();

        switch (id) {
            case Id_constructor:
                // ES2025 Iterator constructor throws TypeError when called
                throw ScriptRuntime.typeError("Iterator is not a constructor");

            case Id_from:
                return js_from(cx, scope, args.length > 0 ? args[0] : Undefined.instance);

            case Id_concat:
                return js_concat(cx, scope, args);

            default:
                // For prototype methods, ensure thisObj is an Iterator
                if (!(thisObj instanceof ES2025IteratorPrototype)) {
                    throw ScriptRuntime.typeError(
                            "Iterator prototype method called on non-Iterator");
                }

                ES2025IteratorPrototype iterator = (ES2025IteratorPrototype) thisObj;

                switch (id) {
                    case Id_next:
                        return iterator.next(cx, scope);
                    case Id_return:
                        return iterator.doReturn(
                                cx, scope, args.length > 0 ? args[0] : Undefined.instance);
                    case Id_throw:
                        return iterator.doThrow(
                                cx, scope, args.length > 0 ? args[0] : Undefined.instance);
                    case SymbolId_iterator:
                        return thisObj;
                    default:
                        // Iterator helper methods - to be implemented
                        throw ScriptRuntime.notFunctionError("Iterator." + f.getFunctionName());
                }
        }
    }

    private static Object js_from(Context cx, Scriptable scope, Object item) {
        // Iterator.from(item) implementation
        // If item is already an iterator with proper prototype, return it
        // Otherwise wrap it in an iterator

        if (item == null || item == Undefined.instance) {
            throw ScriptRuntime.typeError("Cannot convert undefined or null to iterator");
        }

        // Check if it's already an iterator that inherits from Iterator.prototype
        if (item instanceof ES2025IteratorPrototype) {
            return item;
        }

        // Check for Symbol.iterator method
        if (item instanceof Scriptable) {
            Scriptable scriptable = (Scriptable) item;
            Object iteratorMethod = ScriptableObject.getProperty(scriptable, SymbolKey.ITERATOR);

            if (iteratorMethod != Scriptable.NOT_FOUND && iteratorMethod instanceof Callable) {
                // Call @@iterator to get iterator
                Callable callable = (Callable) iteratorMethod;
                Object iterator = callable.call(cx, scope, scriptable, ScriptRuntime.emptyArgs);

                // Wrap the iterator to inherit from Iterator.prototype
                return new WrappedIterator(cx, scope, iterator);
            }
        }

        throw ScriptRuntime.typeError("Object is not iterable");
    }

    private static Object js_concat(Context cx, Scriptable scope, Object[] args) {
        // Iterator.concat(...items) implementation
        // Creates an iterator that yields values from all provided iterators in sequence
        throw ScriptRuntime.notFunctionError("Iterator.concat");
    }

    @Override
    protected int findPrototypeId(Symbol k) {
        if (SymbolKey.ITERATOR.equals(k)) {
            return SymbolId_iterator;
        } else if (SymbolKey.TO_STRING_TAG.equals(k)) {
            return SymbolId_toStringTag;
        }
        return 0;
    }

    @Override
    protected int findPrototypeId(String s) {
        switch (s) {
            case "constructor":
                return Id_constructor;
            case "next":
                return Id_next;
            case "return":
                return Id_return;
            case "throw":
                return Id_throw;
            case "map":
                return Id_map;
            case "filter":
                return Id_filter;
            case "take":
                return Id_take;
            case "drop":
                return Id_drop;
            case "flatMap":
                return Id_flatMap;
            case "reduce":
                return Id_reduce;
            case "toArray":
                return Id_toArray;
            case "forEach":
                return Id_forEach;
            case "some":
                return Id_some;
            case "every":
                return Id_every;
            case "find":
                return Id_find;
        }
        return 0;
    }

    /** Base class for iterators that inherit from Iterator.prototype */
    abstract static class ES2025IteratorPrototype extends ScriptableObject {

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
}
