/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Context-safe utilities for iterator processing. All methods take Context as parameter instead of
 * capturing it, ensuring thread safety.
 */
public final class IteratorOperations {

    private IteratorOperations() {
        // Utility class - no instances
    }

    /**
     * Process iterator and collect all values into array.
     *
     * @param cx Context for iteration
     * @param scope execution scope
     * @param iteratorObj the iterator object with next() method
     * @return array of all values from iterator
     */
    public static Scriptable collectToArray(Context cx, Scriptable scope, Object iteratorObj) {
        List<Object> values = collectToList(cx, scope, iteratorObj);
        return cx.newArray(scope, values.toArray());
    }

    /**
     * Process iterator and collect all values into List.
     *
     * @param cx Context for iteration
     * @param scope execution scope
     * @param iteratorObj the iterator object with next() method
     * @return List of all values from iterator
     */
    public static List<Object> collectToList(Context cx, Scriptable scope, Object iteratorObj) {
        List<Object> values = new ArrayList<>();
        forEach(cx, scope, iteratorObj, values::add);
        return values;
    }

    /**
     * Process iterator calling consumer for each value.
     *
     * @param cx Context for iteration
     * @param scope execution scope
     * @param iteratorObj the iterator object with next() method
     * @param consumer function to call for each value
     */
    public static void forEach(
            Context cx, Scriptable scope, Object iteratorObj, Consumer<Object> consumer) {
        if (!(iteratorObj instanceof Scriptable)) {
            throw ScriptRuntime.typeErrorById("msg.not.iterable", iteratorObj);
        }

        Scriptable iterator = (Scriptable) iteratorObj;

        // Get next method
        Object nextProp = ScriptableObject.getProperty(iterator, "next");
        if (!(nextProp instanceof Callable)) {
            throw ScriptRuntime.typeErrorById("msg.not.iterable", iterator);
        }
        Callable nextMethod = (Callable) nextProp;

        // Get return method for cleanup
        Object returnProp = ScriptableObject.getProperty(iterator, "return");
        Callable returnMethod = (returnProp instanceof Callable) ? (Callable) returnProp : null;

        try {
            while (true) {
                // Call next() method
                Object iterResult = nextMethod.call(cx, scope, iterator, ScriptRuntime.emptyArgs);

                if (!(iterResult instanceof Scriptable)) {
                    throw ScriptRuntime.typeErrorById(
                            "msg.arg.not.object", ScriptRuntime.typeof(iterResult));
                }

                Scriptable result = (Scriptable) iterResult;
                Object done = ScriptableObject.getProperty(result, "done");

                if (ScriptRuntime.toBoolean(done)) {
                    break; // Iterator exhausted
                }

                Object value = ScriptableObject.getProperty(result, "value");
                value = (value == Scriptable.NOT_FOUND) ? Undefined.instance : value;

                consumer.accept(value);
            }
        } catch (Exception e) {
            // Cleanup on error
            if (returnMethod != null) {
                try {
                    returnMethod.call(cx, scope, iterator, ScriptRuntime.emptyArgs);
                } catch (Exception cleanupError) {
                    // Ignore cleanup errors
                }
            }
            throw e;
        }
    }

    /**
     * Check if object can be iterated.
     *
     * @param cx Context for checking
     * @param scope execution scope
     * @param obj object to check
     * @return true if object has iterator method
     */
    public static boolean isIterable(Context cx, Scriptable scope, Object obj) {
        if (!(obj instanceof Scriptable)) {
            return false;
        }

        Object iteratorMethod = ScriptableObject.getProperty((Scriptable) obj, SymbolKey.ITERATOR);
        return (iteratorMethod instanceof Callable);
    }

    /**
     * Get iterator from iterable object.
     *
     * @param cx Context for getting iterator
     * @param scope execution scope
     * @param iterable the iterable object
     * @return iterator object
     */
    public static Object getIterator(Context cx, Scriptable scope, Object iterable) {
        return ScriptRuntime.callIterator(iterable, cx, scope);
    }
}
