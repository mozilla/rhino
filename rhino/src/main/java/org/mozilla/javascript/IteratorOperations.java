/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Context-safe utility methods for working with ECMAScript iterators. All methods take Context as a
 * parameter instead of storing it, ensuring thread safety.
 */
public class IteratorOperations {

    private IteratorOperations() {
        // Utility class, no instances
    }

    /**
     * Collects all values from an iterator into an array. Properly handles cleanup via iterator's
     * return() method.
     *
     * @param cx Current context
     * @param scope Current scope
     * @param iterator The iterator object
     * @return Array containing all iterator values
     */
    public static Object[] collectToArray(Context cx, Scriptable scope, Object iterator) {
        List<Object> result = collectToList(cx, scope, iterator);
        return result.toArray(new Object[result.size()]);
    }

    /**
     * Collects all values from an iterator into a List. Properly handles cleanup via iterator's
     * return() method.
     *
     * @param cx Current context
     * @param scope Current scope
     * @param iterator The iterator object
     * @return List containing all iterator values
     */
    public static List<Object> collectToList(Context cx, Scriptable scope, Object iterator) {
        List<Object> result = new ArrayList<>();

        IteratorLikeIterable iterable = null;
        try {
            iterable = new IteratorLikeIterable(cx, scope, iterator);

            // Use the Context-safe iterator method
            var iter = iterable.iterator();
            while (iter.hasNext()) {
                result.add(iter.next());
            }
        } finally {
            // Ensure cleanup via return() method
            if (iterable != null) {
                iterable.close();
            }
        }

        return result;
    }

    /**
     * Processes each value from an iterator with a consumer function. Properly handles cleanup via
     * iterator's return() method.
     *
     * @param cx Current context
     * @param scope Current scope
     * @param iterator The iterator object
     * @param consumer Function to process each value (receives index and value)
     */
    public static void forEach(
            Context cx, Scriptable scope, Object iterator, BiConsumer<Integer, Object> consumer) {
        IteratorLikeIterable iterable = null;
        try {
            iterable = new IteratorLikeIterable(cx, scope, iterator);

            var iter = iterable.iterator();
            int index = 0;
            while (iter.hasNext()) {
                consumer.accept(index++, iter.next());
            }
        } finally {
            // Ensure cleanup via return() method
            if (iterable != null) {
                iterable.close();
            }
        }
    }

    /**
     * Checks if an object is iterable (has Symbol.iterator method).
     *
     * @param cx Current context
     * @param scope Current scope
     * @param obj Object to check
     * @return true if object is iterable
     */
    public static boolean isIterable(Context cx, Scriptable scope, Object obj) {
        if (obj == null || obj == Undefined.instance) {
            return false;
        }

        if (!(obj instanceof Scriptable)) {
            return false;
        }

        Scriptable scriptable = (Scriptable) obj;
        Object iteratorMethod = ScriptableObject.getProperty(scriptable, SymbolKey.ITERATOR);

        return iteratorMethod != Scriptable.NOT_FOUND && iteratorMethod instanceof Callable;
    }

    /**
     * Gets an iterator from an iterable object.
     *
     * @param cx Current context
     * @param scope Current scope
     * @param iterable The iterable object
     * @return The iterator object
     * @throws JavaScriptException if object is not iterable
     */
    public static Object getIterator(Context cx, Scriptable scope, Object iterable) {
        if (!isIterable(cx, scope, iterable)) {
            throw ScriptRuntime.typeError("Object is not iterable");
        }

        Scriptable scriptable = (Scriptable) iterable;
        Object iteratorMethod = ScriptableObject.getProperty(scriptable, SymbolKey.ITERATOR);

        if (iteratorMethod instanceof Callable) {
            Callable callable = (Callable) iteratorMethod;
            return callable.call(cx, scope, scriptable, ScriptRuntime.emptyArgs);
        }

        throw ScriptRuntime.typeError("@@iterator is not callable");
    }

    /**
     * Creates an iterator result object { done, value }.
     *
     * @param cx Current context
     * @param scope Current scope
     * @param done Whether iteration is complete
     * @param value The value (can be Undefined.instance)
     * @return Iterator result object
     */
    public static Scriptable makeIteratorResult(
            Context cx, Scriptable scope, boolean done, Object value) {
        Scriptable result = cx.newObject(scope);
        ScriptableObject.putProperty(result, ES6Iterator.DONE_PROPERTY, done);
        ScriptableObject.putProperty(result, ES6Iterator.VALUE_PROPERTY, value);
        return result;
    }

    /**
     * Calls the return() method on an iterator if it exists.
     *
     * @param cx Current context
     * @param scope Current scope
     * @param iterator The iterator object
     * @return The result of calling return(), or undefined if no return method
     */
    public static Object callReturn(Context cx, Scriptable scope, Object iterator) {
        if (!(iterator instanceof Scriptable)) {
            return Undefined.instance;
        }

        Scriptable iterScriptable = (Scriptable) iterator;
        Object returnMethod =
                ScriptableObject.getProperty(iterScriptable, ES6Iterator.RETURN_METHOD);

        if (returnMethod instanceof Callable) {
            Callable callable = (Callable) returnMethod;
            return callable.call(cx, scope, iterScriptable, ScriptRuntime.emptyArgs);
        }

        return Undefined.instance;
    }

    /**
     * Gets the next value from an iterator.
     *
     * @param cx Current context
     * @param scope Current scope
     * @param iterator The iterator object
     * @return The iterator result object
     */
    public static Object next(Context cx, Scriptable scope, Object iterator) {
        if (!(iterator instanceof Scriptable)) {
            throw ScriptRuntime.typeError("Iterator must be an object");
        }

        Scriptable iterScriptable = (Scriptable) iterator;
        Object nextMethod = ScriptableObject.getProperty(iterScriptable, ES6Iterator.NEXT_METHOD);

        if (!(nextMethod instanceof Callable)) {
            throw ScriptRuntime.typeError("Iterator missing next method");
        }

        Callable callable = (Callable) nextMethod;
        return callable.call(cx, scope, iterScriptable, ScriptRuntime.emptyArgs);
    }
}
