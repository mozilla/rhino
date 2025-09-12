/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * Wrapper for iterators returned by Iterator.from(). This class wraps any iterator and ensures it
 * inherits from Iterator.prototype.
 *
 * <p>Following Rhino patterns, this extends ES6Iterator and delegates all operations to the wrapped
 * iterator object.
 */
public final class IteratorWrapper extends ES6Iterator {
    private static final long serialVersionUID = 1L;
    private static final String ITERATOR_TAG = "IteratorWrapper";

    private Scriptable wrappedIterator;
    private Object currentValue = Undefined.instance;
    private boolean done = false;

    static void init(ScriptableObject scope, boolean sealed) {
        ES6Iterator.init(scope, sealed, new IteratorWrapper(), ITERATOR_TAG);
    }

    /** Only for constructing the prototype object. */
    private IteratorWrapper() {
        super();
    }

    public IteratorWrapper(Scriptable scope, Scriptable wrappedIterator) {
        super(scope, ITERATOR_TAG);
        this.wrappedIterator = wrappedIterator;

        // Override prototype to inherit from Iterator.prototype if available
        Scriptable iteratorPrototype = NativeIteratorConstructor.getIteratorPrototype(scope);
        if (iteratorPrototype != null) {
            setPrototype(iteratorPrototype);
        }
    }

    @Override
    public String getClassName() {
        return "Iterator";
    }

    @Override
    protected boolean isDone(Context cx, Scriptable scope) {
        return done || exhausted;
    }

    @Override
    protected Object nextValue(Context cx, Scriptable scope) {
        if (done || exhausted) {
            return Undefined.instance;
        }

        // Call next() on the wrapped iterator
        Object nextMethod = ScriptableObject.getProperty(wrappedIterator, "next");
        if (!(nextMethod instanceof Callable)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.isnt.function", "next", ScriptRuntime.typeof(nextMethod));
        }

        Callable nextFunc = (Callable) nextMethod;
        Object result = nextFunc.call(cx, scope, wrappedIterator, ScriptRuntime.emptyArgs);

        // Check if result is an object
        if (!(result instanceof Scriptable)) {
            throw ScriptRuntime.typeErrorById("msg.iterator.primitive");
        }

        Scriptable resultObj = (Scriptable) result;

        // Check done property
        Object doneValue = ScriptableObject.getProperty(resultObj, DONE_PROPERTY);
        done = ScriptRuntime.toBoolean(doneValue);

        if (done) {
            exhausted = true;
        }

        // Get the value
        currentValue = ScriptableObject.getProperty(resultObj, VALUE_PROPERTY);
        return currentValue;
    }

    @Override
    protected String getTag() {
        return ITERATOR_TAG;
    }

    @Override
    public Object execIdCall(
            IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(getTag())) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();

        // Handle return and throw methods if they exist
        if (id == Id_return) {
            return doReturn(cx, scope, thisObj, args.length > 0 ? args[0] : Undefined.instance);
        } else if (id == Id_throw) {
            return doThrow(cx, scope, thisObj, args.length > 0 ? args[0] : Undefined.instance);
        }

        return super.execIdCall(f, cx, scope, thisObj, args);
    }

    @Override
    protected void initPrototypeId(int id) {
        if (id == Id_return) {
            initPrototypeMethod(getTag(), id, RETURN_METHOD, 1);
            return;
        } else if (id == Id_throw) {
            initPrototypeMethod(getTag(), id, "throw", 1);
            return;
        }
        super.initPrototypeId(id);
    }

    @Override
    protected int findPrototypeId(String s) {
        if (RETURN_METHOD.equals(s)) {
            return Id_return;
        } else if ("throw".equals(s)) {
            return Id_throw;
        }
        return super.findPrototypeId(s);
    }

    private Object doReturn(Context cx, Scriptable scope, Scriptable thisObj, Object value) {
        IteratorWrapper self = ensureType(thisObj, IteratorWrapper.class, "return");

        // Mark as exhausted
        self.exhausted = true;
        self.done = true;

        // Check if wrapped iterator has return method
        Object returnMethod = ScriptableObject.getProperty(self.wrappedIterator, RETURN_METHOD);

        if (returnMethod == Scriptable.NOT_FOUND
                || returnMethod == null
                || Undefined.isUndefined(returnMethod)) {
            // No return method - return completion
            return makeIteratorResult(cx, scope, Boolean.TRUE, value);
        }

        if (!(returnMethod instanceof Callable)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.isnt.function", RETURN_METHOD, ScriptRuntime.typeof(returnMethod));
        }

        Callable returnFunc = (Callable) returnMethod;
        Object result = returnFunc.call(cx, scope, self.wrappedIterator, new Object[] {value});

        // Validate the result is an object
        if (!(result instanceof Scriptable)) {
            throw ScriptRuntime.typeErrorById("msg.iterator.primitive");
        }

        return result;
    }

    private Object doThrow(Context cx, Scriptable scope, Scriptable thisObj, Object exception) {
        IteratorWrapper self = ensureType(thisObj, IteratorWrapper.class, "throw");

        // Check if wrapped iterator has throw method
        Object throwMethod = ScriptableObject.getProperty(self.wrappedIterator, "throw");

        if (throwMethod == Scriptable.NOT_FOUND
                || throwMethod == null
                || Undefined.isUndefined(throwMethod)) {
            // No throw method - mark as exhausted and throw
            self.exhausted = true;
            self.done = true;
            throw ScriptRuntime.throwError(cx, scope, exception.toString());
        }

        if (!(throwMethod instanceof Callable)) {
            throw ScriptRuntime.typeErrorById(
                    "msg.isnt.function", "throw", ScriptRuntime.typeof(throwMethod));
        }

        Callable throwFunc = (Callable) throwMethod;
        Object result = throwFunc.call(cx, scope, self.wrappedIterator, new Object[] {exception});

        // Check if iterator is done after throw
        if (result instanceof Scriptable) {
            Object doneValue = ScriptableObject.getProperty((Scriptable) result, DONE_PROPERTY);
            if (ScriptRuntime.toBoolean(doneValue)) {
                self.exhausted = true;
                self.done = true;
            }
        }

        return result;
    }

    // Additional prototype IDs for return and throw methods
    private static final int Id_return = 4; // After SymbolId_toStringTag which is 3
    private static final int Id_throw = 5;
    private static final int WRAPPER_MAX_PROTOTYPE_ID = Id_throw;
}
