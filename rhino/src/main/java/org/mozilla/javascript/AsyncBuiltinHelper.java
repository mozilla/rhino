/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.mozilla.javascript;

/**
 * Helper class for implementing async built-in functions in Rhino.
 *
 * <p>This class provides reusable patterns for implementing ES2024+ async operations that require
 * sequential promise processing, similar to "for await...of" semantics.
 *
 * <p>Since Rhino doesn't support async/await syntax, this helper manages the complex promise
 * chaining required to implement sequential async iteration correctly.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Sequential processing (one value at a time, not parallel)
 *   <li>Lazy evaluation (stops at first error)
 *   <li>Proper error propagation through promise rejection
 *   <li>Support for both iterables and array-like objects
 *   <li>Optional value transformation via mapper functions
 *   <li>Compatible with both legacy and ES2025 Iterator implementations
 * </ul>
 *
 * <p>This helper works with both:
 *
 * <ul>
 *   <li>Legacy Iterator (callable/constructable) for backward compatibility
 *   <li>ES2025 Iterator (throws on call/construct) when FEATURE_ES2025_ITERATOR is enabled
 * </ul>
 *
 * <p>The IteratorLikeIterable class handles the differences transparently, so this helper doesn't
 * need to check the iterator mode.
 *
 * @see IteratorLikeIterable
 * @see NativeArray#js_fromAsync
 */
public class AsyncBuiltinHelper {

    /**
     * Interface for processing async values. Implementations define how each value should be
     * handled.
     */
    public interface AsyncValueProcessor {
        /**
         * Process a single resolved value.
         *
         * @param cx the Context
         * @param scope the scope
         * @param value the resolved value
         * @param index the index of the value
         * @return the processed value to store
         */
        Object processValue(Context cx, Scriptable scope, Object value, long index);
    }

    /**
     * Interface for collecting processed values. Implementations define how results are
     * accumulated.
     */
    public interface ResultCollector {
        /**
         * Store a processed value in the result.
         *
         * @param cx the Context
         * @param value the value to store
         * @param index the index where to store
         */
        void collect(Context cx, Object value, long index);

        /**
         * Finalize the result collection.
         *
         * @param cx the Context
         * @param totalCount the total number of items processed
         * @return the final result object
         */
        Object finalize(Context cx, long totalCount);
    }

    /**
     * Create a promise that resolves after sequential async iteration.
     *
     * @param cx the Context
     * @param scope the scope
     * @param executor the executor function for the promise
     * @return a new Promise
     */
    public static Object createAsyncPromise(Context cx, Scriptable scope, Callable executor) {

        Object promiseCtor = ScriptableObject.getProperty(scope, "Promise");
        if (!(promiseCtor instanceof Function)) {
            throw ScriptRuntime.typeErrorById("msg.no.promise");
        }

        return ((Function) promiseCtor).construct(cx, scope, new Object[] {executor});
    }

    /**
     * Process an iterable sequentially through promises.
     *
     * @param cx the Context
     * @param scope the scope
     * @param iterable the iterable to process
     * @param processor the value processor
     * @param collector the result collector
     * @param resolve promise resolve function
     * @param reject promise reject function
     */
    public static void processIterableAsync(
            Context cx,
            Scriptable scope,
            IteratorLikeIterable iterable,
            AsyncValueProcessor processor,
            ResultCollector collector,
            Function resolve,
            Function reject) {

        processIterableAsyncInternal(cx, scope, iterable, processor, collector, 0, resolve, reject);
    }

    private static void processIterableAsyncInternal(
            Context cx,
            Scriptable scope,
            IteratorLikeIterable iterable,
            AsyncValueProcessor processor,
            ResultCollector collector,
            long index,
            Function resolve,
            Function reject) {

        try {
            // Check if iterator has more values
            if (!iterable.iterator().hasNext()) {
                // Done iterating
                Object result = collector.finalize(cx, index);
                resolve.call(cx, scope, null, new Object[] {result});
                iterable.close();
                return;
            }

            // Get next value
            Object value = iterable.iterator().next();

            // Process through promise chain
            processValueAsync(
                    cx,
                    scope,
                    value,
                    index,
                    (resolvedValue) -> {
                        try {
                            // Process the value
                            Object processed =
                                    processor.processValue(cx, scope, resolvedValue, index);

                            // Collect the result
                            collector.collect(cx, processed, index);

                            // Continue with next value
                            processIterableAsyncInternal(
                                    cx, scope, iterable, processor, collector, index + 1, resolve,
                                    reject);
                        } catch (Exception e) {
                            iterable.close();
                            reject.call(cx, scope, null, new Object[] {e});
                        }
                    },
                    (error) -> {
                        // Stop on first error
                        iterable.close();
                        reject.call(cx, scope, null, new Object[] {error});
                    });

        } catch (Exception e) {
            iterable.close();
            reject.call(cx, scope, null, new Object[] {e});
        }
    }

    /**
     * Process an array-like object sequentially through promises.
     *
     * @param cx the Context
     * @param scope the scope
     * @param source the array-like source object
     * @param length the length of the array-like object
     * @param processor the value processor
     * @param collector the result collector
     * @param resolve promise resolve function
     * @param reject promise reject function
     */
    public static void processArrayLikeAsync(
            Context cx,
            Scriptable scope,
            Scriptable source,
            long length,
            AsyncValueProcessor processor,
            ResultCollector collector,
            Function resolve,
            Function reject) {

        processArrayLikeAsyncInternal(
                cx, scope, source, length, processor, collector, 0, resolve, reject);
    }

    private static void processArrayLikeAsyncInternal(
            Context cx,
            Scriptable scope,
            Scriptable source,
            long length,
            AsyncValueProcessor processor,
            ResultCollector collector,
            long index,
            Function resolve,
            Function reject) {

        // Check if we've processed all elements
        if (index >= length) {
            Object result = collector.finalize(cx, length);
            resolve.call(cx, scope, null, new Object[] {result});
            return;
        }

        try {
            // Get value at current index
            Object value = ScriptableObject.getProperty(source, (int) index);

            // Process through promise chain
            processValueAsync(
                    cx,
                    scope,
                    value,
                    index,
                    (resolvedValue) -> {
                        try {
                            // Process the value
                            Object processed =
                                    processor.processValue(cx, scope, resolvedValue, index);

                            // Collect the result
                            collector.collect(cx, processed, index);

                            // Continue with next index
                            processArrayLikeAsyncInternal(
                                    cx, scope, source, length, processor, collector, index + 1,
                                    resolve, reject);
                        } catch (Exception e) {
                            reject.call(cx, scope, null, new Object[] {e});
                        }
                    },
                    (error) -> {
                        // Stop on first error
                        reject.call(cx, scope, null, new Object[] {error});
                    });

        } catch (Exception e) {
            reject.call(cx, scope, null, new Object[] {e});
        }
    }

    /**
     * Interface for async value handlers. (Note: Cannot use @FunctionalInterface due to Java 11
     * compatibility)
     */
    private interface AsyncHandler {
        void handle(Object value);
    }

    /**
     * Process a single value through promise resolution. This handles both promise and non-promise
     * values.
     */
    private static void processValueAsync(
            Context cx,
            Scriptable scope,
            Object value,
            long index,
            AsyncHandler onSuccess,
            AsyncHandler onError) {

        // Convert to promise
        Object promise = promiseResolve(cx, scope, value);

        if (!(promise instanceof Scriptable)) {
            // Not a promise, handle synchronously
            onSuccess.handle(value);
            return;
        }

        // Get the 'then' method
        Object thenMethod = ScriptableObject.getProperty((Scriptable) promise, "then");
        if (!(thenMethod instanceof Function)) {
            // Not a thenable, treat as resolved value
            onSuccess.handle(promise);
            return;
        }

        // Create success handler
        Callable onFulfilled =
                new LambdaFunction(
                        scope,
                        1,
                        (Context ctx, Scriptable s, Scriptable thisObj, Object[] args) -> {
                            Object resolvedValue = args.length > 0 ? args[0] : Undefined.instance;
                            onSuccess.handle(resolvedValue);
                            return Undefined.instance;
                        });

        // Create error handler
        Callable onRejected =
                new LambdaFunction(
                        scope,
                        1,
                        (Context ctx, Scriptable s, Scriptable thisObj, Object[] args) -> {
                            Object error = args.length > 0 ? args[0] : Undefined.instance;
                            onError.handle(error);
                            return Undefined.instance;
                        });

        // Chain the promise
        ((Function) thenMethod)
                .call(cx, scope, (Scriptable) promise, new Object[] {onFulfilled, onRejected});
    }

    /** Convert a value to a promise using Promise.resolve. */
    private static Object promiseResolve(Context cx, Scriptable scope, Object value) {
        Object promiseCtor = ScriptableObject.getProperty(scope, "Promise");
        if (promiseCtor instanceof Function) {
            Object resolveMethod =
                    ScriptableObject.getProperty((Scriptable) promiseCtor, "resolve");
            if (resolveMethod instanceof Function) {
                return ((Function) resolveMethod)
                        .call(cx, scope, (Scriptable) promiseCtor, new Object[] {value});
            }
        }
        return value;
    }

    /**
     * Create a simple array result collector.
     *
     * @param result the result array to populate
     * @return a ResultCollector that populates the array
     */
    public static ResultCollector createArrayCollector(final Scriptable result) {
        return new ResultCollector() {
            @Override
            public void collect(Context cx, Object value, long index) {
                ArrayLikeAbstractOperations.defineElem(cx, result, index, value);
            }

            @Override
            public Object finalize(Context cx, long totalCount) {
                ScriptableObject.putProperty(result, "length", totalCount);
                return result;
            }
        };
    }

    /**
     * Create a value processor with an optional mapper function.
     *
     * @param mapFn the mapper function (may be null)
     * @param thisArg the 'this' argument for the mapper
     * @return an AsyncValueProcessor
     */
    public static AsyncValueProcessor createMappingProcessor(
            final Function mapFn, final Scriptable thisArg) {

        if (mapFn == null) {
            // No mapping, return values as-is
            return (cx, scope, value, index) -> value;
        }

        // Apply the mapper function
        return (cx, scope, value, index) ->
                mapFn.call(cx, scope, thisArg, new Object[] {value, Long.valueOf(index)});
    }
}
