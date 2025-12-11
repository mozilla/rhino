/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * Adapter that wraps any iterator to inherit from Iterator.prototype. Used by Iterator.from() to
 * ensure returned iterators have proper prototype chain. All operations are delegated to the
 * wrapped iterator.
 */
public class ES6IteratorAdapter extends ScriptableObject {

    private static final long serialVersionUID = 1L;

    private final Scriptable wrappedIterator;
    private final Callable nextMethod;
    private Callable returnMethod;
    private Callable throwMethod;

    /**
     * Creates an adapter that wraps an iterator to inherit from Iterator.prototype.
     *
     * @param cx Current context
     * @param scope Current scope
     * @param iterator The iterator to wrap
     */
    public ES6IteratorAdapter(Context cx, Scriptable scope, Object iterator) {
        if (!(iterator instanceof Scriptable)) {
            throw ScriptRuntime.typeError("Iterator must be an object");
        }

        this.wrappedIterator = (Scriptable) iterator;

        // Get the next method (required)
        Object next = ScriptableObject.getProperty(wrappedIterator, ES6Iterator.NEXT_METHOD);
        if (!(next instanceof Callable)) {
            throw ScriptRuntime.typeError("Iterator missing next method");
        }
        this.nextMethod = (Callable) next;

        // Get optional return method
        Object ret = ScriptableObject.getProperty(wrappedIterator, ES6Iterator.RETURN_METHOD);
        if (ret instanceof Callable) {
            this.returnMethod = (Callable) ret;
        }

        // Get optional throw method
        Object thr = ScriptableObject.getProperty(wrappedIterator, "throw");
        if (thr instanceof Callable) {
            this.throwMethod = (Callable) thr;
        }

        // Set up prototype chain to inherit from Iterator.prototype
        setupPrototype(cx, scope);
    }

    private void setupPrototype(Context cx, Scriptable scope) {
        Scriptable topScope = ScriptableObject.getTopLevelScope(scope);

        // Try to find Iterator.prototype
        Object iteratorCtor = ScriptableObject.getProperty(topScope, "Iterator");
        if (iteratorCtor instanceof Scriptable) {
            Object proto = ScriptableObject.getProperty((Scriptable) iteratorCtor, "prototype");
            if (proto instanceof Scriptable) {
                this.setPrototype((Scriptable) proto);
            }
        }

        this.setParentScope(scope);
    }

    @Override
    public String getClassName() {
        return "Iterator";
    }

    @Override
    public Object get(String name, Scriptable start) {
        // First check if we have the property
        Object result = super.get(name, start);
        if (result != NOT_FOUND) {
            return result;
        }

        // Delegate property access to wrapped iterator
        return ScriptableObject.getProperty(wrappedIterator, name);
    }

    @Override
    public Object get(Symbol key, Scriptable start) {
        // First check if we have the property
        Object result = super.get(key, start);
        if (result != NOT_FOUND) {
            return result;
        }

        // Delegate symbol property access to wrapped iterator
        if (wrappedIterator instanceof SymbolScriptable) {
            return ((SymbolScriptable) wrappedIterator).get(key, start);
        }
        return NOT_FOUND;
    }

    @Override
    public boolean has(String name, Scriptable start) {
        // Check both our properties and wrapped iterator's properties
        return super.has(name, start) || ScriptableObject.hasProperty(wrappedIterator, name);
    }

    @Override
    public boolean has(Symbol key, Scriptable start) {
        // Check both our properties and wrapped iterator's properties
        if (super.has(key, start)) {
            return true;
        }

        if (wrappedIterator instanceof SymbolScriptable) {
            return ((SymbolScriptable) wrappedIterator).has(key, start);
        }
        return false;
    }

    /**
     * Calls the next() method on the wrapped iterator.
     *
     * @param cx Current context
     * @param scope Current scope
     * @return Iterator result object
     */
    public Object next(Context cx, Scriptable scope) {
        return nextMethod.call(cx, scope, wrappedIterator, ScriptRuntime.emptyArgs);
    }

    /**
     * Calls the return() method on the wrapped iterator if it exists.
     *
     * @param cx Current context
     * @param scope Current scope
     * @param value Return value
     * @return Iterator result object
     */
    public Object doReturn(Context cx, Scriptable scope, Object value) {
        if (returnMethod != null) {
            return returnMethod.call(cx, scope, wrappedIterator, new Object[] {value});
        }

        // Default behavior if no return method
        return IteratorOperations.makeIteratorResult(cx, scope, true, value);
    }

    /**
     * Calls the throw() method on the wrapped iterator if it exists.
     *
     * @param cx Current context
     * @param scope Current scope
     * @param value Value to throw
     * @return Iterator result object (if throw method handles it)
     * @throws JavaScriptException if no throw method or it rethrows
     */
    public Object doThrow(Context cx, Scriptable scope, Object value) {
        if (throwMethod != null) {
            return throwMethod.call(cx, scope, wrappedIterator, new Object[] {value});
        }

        // Default behavior if no throw method - throw the value
        if (value instanceof JavaScriptException) {
            throw (JavaScriptException) value;
        } else if (value instanceof RhinoException) {
            throw (RhinoException) value;
        }
        throw ScriptRuntime.typeError(value.toString());
    }

    /**
     * Gets the wrapped iterator object.
     *
     * @return The wrapped iterator
     */
    public Scriptable getWrappedIterator() {
        return wrappedIterator;
    }
}
