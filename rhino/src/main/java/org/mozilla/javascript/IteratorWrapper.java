/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * Wrapper for iterators returned by Iterator.from(). This class wraps any iterator and ensures it
 * inherits from Iterator.prototype.
 */
public class IteratorWrapper extends ScriptableObject {
    private static final long serialVersionUID = 1L;
    private static final String ITERATOR_TAG = "IteratorWrapper";

    private Scriptable wrappedIterator;
    private boolean exhausted = false;

    public IteratorWrapper() {}

    public IteratorWrapper(Scriptable iterator, Scriptable scope) {
        this.wrappedIterator = iterator;

        // Set up proper prototype chain - inherit from Iterator.prototype
        Scriptable iteratorPrototype = NativeIteratorConstructor.getIteratorPrototype(scope);
        if (iteratorPrototype != null) {
            this.setPrototype(iteratorPrototype);
        }
        this.setParentScope(scope);

        // Define the next method
        defineProperty(
                "next",
                new BaseFunction() {
                    @Override
                    public Object call(
                            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                        if (thisObj instanceof IteratorWrapper) {
                            return ((IteratorWrapper) thisObj).next(cx, scope, args);
                        }
                        throw ScriptRuntime.typeErrorById("msg.incompat.call", "next");
                    }

                    @Override
                    public String getFunctionName() {
                        return "next";
                    }
                },
                DONTENUM);

        // Define the return method
        defineProperty(
                "return",
                new BaseFunction() {
                    @Override
                    public Object call(
                            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                        if (thisObj instanceof IteratorWrapper) {
                            return ((IteratorWrapper) thisObj)
                                    .doReturn(
                                            cx,
                                            scope,
                                            args.length > 0 ? args[0] : Undefined.instance);
                        }
                        throw ScriptRuntime.typeErrorById("msg.incompat.call", "return");
                    }

                    @Override
                    public String getFunctionName() {
                        return "return";
                    }
                },
                DONTENUM);

        // Define the throw method
        defineProperty(
                "throw",
                new BaseFunction() {
                    @Override
                    public Object call(
                            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                        if (thisObj instanceof IteratorWrapper) {
                            return ((IteratorWrapper) thisObj)
                                    .doThrow(
                                            cx,
                                            scope,
                                            args.length > 0 ? args[0] : Undefined.instance);
                        }
                        throw ScriptRuntime.typeErrorById("msg.incompat.call", "throw");
                    }

                    @Override
                    public String getFunctionName() {
                        return "throw";
                    }
                },
                DONTENUM);
    }

    @Override
    public String getClassName() {
        return "Iterator";
    }

    /** Implements the next() method by delegating to the wrapped iterator. */
    public Object next(Context cx, Scriptable scope, Object[] args) {
        if (exhausted) {
            return makeIteratorResult(cx, scope, true, Undefined.instance);
        }

        // Call next() on the wrapped iterator
        Object nextMethod = ScriptableObject.getProperty(wrappedIterator, "next");
        if (!(nextMethod instanceof Callable)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.isnt.function", "next", ScriptRuntime.typeof(nextMethod));
        }

        Callable nextFunc = (Callable) nextMethod;
        Object result = nextFunc.call(cx, scope, wrappedIterator, args);

        // Check if result is an object
        if (!(result instanceof Scriptable)) {
            throw ScriptRuntime.typeErrorById("msg.iterator.primitive");
        }

        // Check done property
        Object done = ScriptableObject.getProperty((Scriptable) result, "done");
        if (ScriptRuntime.toBoolean(done)) {
            exhausted = true;
        }

        return result;
    }

    /** Implements the return() method by delegating to the wrapped iterator if it has one. */
    public Object doReturn(Context cx, Scriptable scope, Object value) {
        // Check if wrapped iterator has return method
        Object returnMethod = ScriptableObject.getProperty(wrappedIterator, "return");

        // Mark as exhausted first
        exhausted = true;

        if (returnMethod == Scriptable.NOT_FOUND
                || returnMethod == null
                || Undefined.isUndefined(returnMethod)) {
            // No return method - return completion
            return makeIteratorResult(cx, scope, true, value);
        }

        if (!(returnMethod instanceof Callable)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.isnt.function", "return", ScriptRuntime.typeof(returnMethod));
        }

        Callable returnFunc = (Callable) returnMethod;
        Object result = returnFunc.call(cx, scope, wrappedIterator, new Object[] {value});

        // Validate the result is an object
        if (!(result instanceof Scriptable)) {
            throw ScriptRuntime.typeErrorById("msg.iterator.primitive");
        }

        return result;
    }

    /** Implements the throw() method by delegating to the wrapped iterator if it has one. */
    public Object doThrow(Context cx, Scriptable scope, Object exception) {
        // Check if wrapped iterator has throw method
        Object throwMethod = ScriptableObject.getProperty(wrappedIterator, "throw");
        if (throwMethod == Scriptable.NOT_FOUND
                || throwMethod == null
                || Undefined.isUndefined(throwMethod)) {
            // No throw method - mark as exhausted and throw
            exhausted = true;
            throw ScriptRuntime.throwError(cx, scope, exception.toString());
        }

        if (!(throwMethod instanceof Callable)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.isnt.function", "throw", ScriptRuntime.typeof(throwMethod));
        }

        Callable throwFunc = (Callable) throwMethod;
        Object result = throwFunc.call(cx, scope, wrappedIterator, new Object[] {exception});

        // Check if iterator is done after throw
        if (result instanceof Scriptable) {
            Object done = ScriptableObject.getProperty((Scriptable) result, "done");
            if (ScriptRuntime.toBoolean(done)) {
                exhausted = true;
            }
        }

        return result;
    }

    private static Scriptable makeIteratorResult(
            Context cx, Scriptable scope, boolean done, Object value) {
        Scriptable result = cx.newObject(scope);
        ScriptableObject.putProperty(result, "value", value);
        ScriptableObject.putProperty(result, "done", done);
        return result;
    }

    protected void initPrototypeId(int id) {
        // No prototype methods to initialize - they come from Iterator.prototype
    }
}
