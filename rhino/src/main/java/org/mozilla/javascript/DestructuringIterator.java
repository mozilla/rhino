/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.ArrayList;
import java.util.List;

/**
 * A wrapper around an iterator for use in array destructuring. This allows destructuring to consume
 * iterator values one-by-one on demand, rather than draining values to an array in one shot.
 */
public class DestructuringIterator extends ScriptableObject {
    private static final long serialVersionUID = 1L;

    private final Context cx;
    private final Scriptable scope;
    private final Callable nextFunc;
    private final Scriptable iteratorThis;
    private final Callable returnFunc;
    private final List<Object> cachedValues;
    private boolean done;
    private boolean closed;
    private int elementsNeeded;
    private boolean finalized;

    public DestructuringIterator(Context cx, Scriptable scope, Object iteratorObj) {
        this.cx = cx;
        this.scope = scope;
        this.cachedValues = new ArrayList<>();
        this.done = false;
        this.closed = false;
        this.elementsNeeded = Integer.MAX_VALUE;
        this.finalized = false;

        // Get the "next" function
        ScriptRuntime.LookupResult nextCall =
                ScriptRuntime.getPropAndThis(iteratorObj, ES6Iterator.NEXT_METHOD, cx, scope);
        this.nextFunc = nextCall.getCallable();
        this.iteratorThis = nextCall.getThis();

        // Get the "return" function (may be null/undefined)
        Object retObj =
                ScriptRuntime.getObjectPropNoWarn(
                        iteratorObj, ES6Iterator.RETURN_PROPERTY, cx, scope);
        if ((retObj != null) && !Undefined.isUndefined(retObj)) {
            if (!(retObj instanceof Callable)) {
                throw ScriptRuntime.notFunctionError(
                        iteratorObj, retObj, ES6Iterator.RETURN_PROPERTY);
            }
            this.returnFunc = (Callable) retObj;
        } else {
            this.returnFunc = null;
        }
    }

    @Override
    public String getClassName() {
        return "DestructuringIterator";
    }

    @Override
    public boolean has(int index, Scriptable start) {
        // always true to support lazy fetching
        return true;
    }

    /** Set the number of elements needed for destructuring. */
    public void setElementsNeeded(int count) {
        this.elementsNeeded = count;
    }

    @Override
    public Object get(int index, Scriptable start) {
        // Fetch values up to the requested index
        while (cachedValues.size() <= index && !done) {
            fetchNext();
        }

        // Auto-finalize if we've fetched all needed elements
        if (!finalized && cachedValues.size() >= elementsNeeded) {
            finalizeIfNeeded();
        }

        if (index < cachedValues.size()) {
            return cachedValues.get(index);
        }

        // Index beyond what iterator produced - return undefined
        return Undefined.instance;
    }

    /** Fetch the next value from the iterator. */
    private void fetchNext() {
        if (done) {
            return;
        }

        try {
            Object result = nextFunc.call(cx, scope, iteratorThis, ScriptRuntime.emptyArgs);
            // Check the "done" property
            Object doneVal =
                    ScriptableObject.getProperty(
                            ScriptableObject.ensureScriptable(result), ES6Iterator.DONE_PROPERTY);
            if (doneVal == Scriptable.NOT_FOUND) {
                doneVal = Undefined.instance;
            }

            if (ScriptRuntime.toBoolean(doneVal)) {
                done = true;
                return;
            }

            // Get the value
            Object value =
                    ScriptRuntime.getObjectPropNoWarn(
                            result, ES6Iterator.VALUE_PROPERTY, cx, scope);
            cachedValues.add(value);
        } catch (Exception e) {
            // Error during iteration - close the iterator
            closeIterator();
            throw e;
        }
    }

    /** Close the iterator by calling its return() method. */
    public void closeIterator() {
        if (closed || returnFunc == null) {
            return;
        }

        closed = true;
        try {
            returnFunc.call(cx, scope, iteratorThis, ScriptRuntime.emptyArgs);
        } catch (Exception e) {
            // TODO: ignore errors / should we log a warning?
        }
    }

    /** Finalize destructuring. Close iterator if not exhausted. */
    private void finalizeIfNeeded() {
        if (finalized) {
            return;
        }
        finalized = true;

        // If we're not exhausted after getting what we need, close the iterator
        if (!done) {
            closeIterator();
        }
    }
}
