/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.Closeable;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This is a class that makes it easier to iterate over "iterator-like" objects as defined in the
 * ECMAScript spec. The caller is responsible for retrieving an object that implements the
 * "iterator" pattern. This class will follow that pattern and throw appropriate JavaScript
 * exceptions.
 *
 * <p>The pattern that the target class should follow is: * It must have a function property called
 * "next" * The function must return an object with a boolean value called "done". * If "done" is
 * true, then the returned object should also contain a "value" property. * If it has a function
 * property called "return" then it will be called when the caller is done iterating.
 *
 * <p><strong>Note:</strong> This class now uses IteratorOperations internally for Context safety.
 */
public class IteratorLikeIterable implements Iterable<Object>, Closeable {
    private final Scriptable iterator;
    private final Callable returnFunc;
    private boolean closed;

    public IteratorLikeIterable(Context cx, Scriptable scope, Object target) {
        // Validate target has iterator interface - delegate to IteratorOperations
        if (!(target instanceof Scriptable)) {
            throw ScriptRuntime.typeErrorById("msg.not.iterable", target);
        }
        this.iterator = (Scriptable) target;

        // Validate iterator has 'next' method
        Object nextProp = ScriptableObject.getProperty(iterator, "next");
        if (!(nextProp instanceof Callable)) {
            throw ScriptRuntime.typeErrorById("msg.not.iterable", iterator);
        }

        // Get return method for cleanup
        Object returnProp = ScriptableObject.getProperty(iterator, "return");
        this.returnFunc = (returnProp instanceof Callable) ? (Callable) returnProp : null;
    }

    @Override
    public void close() {
        // Default close - requires Context to be passed for cleanup
        close(Context.getCurrentContext(), iterator.getParentScope());
    }

    /**
     * Close with explicit Context and scope (Context-safe version).
     *
     * @param cx fresh Context for cleanup
     * @param scope scope for cleanup
     */
    public void close(Context cx, Scriptable scope) {
        if (!closed) {
            closed = true;
            if (returnFunc != null && cx != null) {
                returnFunc.call(cx, scope, iterator, ScriptRuntime.emptyArgs);
            }
        }
    }

    @Override
    public Itr iterator() {
        // Default iterator uses current Context (not ideal but compatible)
        return new Itr(Context.getCurrentContext(), iterator.getParentScope());
    }

    /**
     * Create iterator with explicit Context and scope (Context-safe version).
     *
     * @param cx fresh Context for iteration
     * @param scope scope for iteration
     * @return new iterator instance
     */
    public Itr iterator(Context cx, Scriptable scope) {
        return new Itr(cx, scope);
    }

    public final class Itr implements Iterator<Object> {
        private final Context cx;
        private final Scriptable scope;
        private final Callable nextMethod;
        private Object nextVal;
        private boolean isDone;

        private Itr(Context cx, Scriptable scope) {
            this.cx = cx;
            this.scope = scope;

            // Get next method - already validated in constructor
            Object nextProp = ScriptableObject.getProperty(iterator, "next");
            this.nextMethod = (Callable) nextProp;
        }

        @Override
        public boolean hasNext() {
            if (isDone) {
                return false;
            }

            Object iterResult;
            try {
                // Call next() to get the next value - this enables lazy iteration
                // CRITICAL: Errors must propagate immediately per ECMAScript spec
                iterResult = nextMethod.call(cx, scope, iterator, ScriptRuntime.emptyArgs);
            } catch (Exception e) {
                isDone = true;
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new RuntimeException(e);
            }

            if (!(iterResult instanceof Scriptable)) {
                isDone = true;
                throw ScriptRuntime.typeErrorById(
                        "msg.arg.not.object", ScriptRuntime.typeof(iterResult));
            }

            Scriptable result = (Scriptable) iterResult;
            Object done;
            try {
                done = ScriptableObject.getProperty(result, "done");
            } catch (Exception e) {
                isDone = true;
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new RuntimeException(e);
            }

            if (ScriptRuntime.toBoolean(done)) {
                isDone = true;
                return false;
            }

            try {
                nextVal = ScriptableObject.getProperty(result, "value");
                nextVal = (nextVal == Scriptable.NOT_FOUND) ? Undefined.instance : nextVal;
            } catch (Exception e) {
                isDone = true;
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new RuntimeException(e);
            }

            return true;
        }

        @Override
        public Object next() {
            if (isDone) {
                throw new NoSuchElementException();
            }
            return nextVal;
        }

        /** Find out if iterator is exhausted. */
        public boolean isDone() {
            return isDone;
        }

        /** Set iterator to exhausted state for exception handling. */
        public void setDone(boolean done) {
            this.isDone = done;
        }
    }
}
