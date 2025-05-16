/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.RandomAccess;
import org.mozilla.javascript.AbstractEcmaObjectOperations;
import org.mozilla.javascript.ArrayLikeAbstractOperations;
import org.mozilla.javascript.ArrayLikeAbstractOperations.IterativeOperation;
import org.mozilla.javascript.ArrayLikeAbstractOperations.ReduceOperation;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Constructable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ExternalArrayData;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeArrayIterator;
import org.mozilla.javascript.NativeArrayIterator.ARRAY_ITERATOR_TYPE;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.SymbolKey;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;

/**
 * This class is the abstract parent for all of the various typed arrays. Each one shows a view of a
 * specific NativeArrayBuffer, and modifications here will affect the rest.
 */
public abstract class NativeTypedArrayView<T> extends NativeArrayBufferView
        implements List<T>, RandomAccess, ExternalArrayData {
    private static final long serialVersionUID = -4963053773152251274L;

    /** The length, in elements, of the array */
    protected final int length;

    protected NativeTypedArrayView() {
        super();
        length = 0;
    }

    protected NativeTypedArrayView(NativeArrayBuffer ab, int off, int len, int byteLen) {
        super(ab, off, byteLen);
        length = len;
    }

    // Array properties implementation.
    // Typed array objects are "Integer-indexed exotic objects" in the ECMAScript spec.
    // Integer properties, and string properties that can be converted to integer indices,
    // behave differently than in other types of JavaScript objects, in that they are
    // silently ignored (and always valued as "undefined") when they are out of bounds.

    @Override
    public Object get(int index, Scriptable start) {
        return js_get(index);
    }

    @Override
    public Object get(String name, Scriptable start) {
        Optional<Double> num = ScriptRuntime.canonicalNumericIndexString(name);
        if (num.isPresent()) {
            // Now we had a valid number, so no matter what we try to return an array element
            int ix = toIndex(num.get());
            if (ix >= 0) {
                return js_get(ix);
            }
        }
        return super.get(name, start);
    }

    @Override
    public boolean has(int index, Scriptable start) {
        return !checkIndex(index);
    }

    @Override
    public boolean has(String name, Scriptable start) {
        Optional<Double> num = ScriptRuntime.canonicalNumericIndexString(name);
        if (num.isPresent()) {
            int ix = toIndex(num.get());
            if (ix >= 0) {
                return !checkIndex(ix);
            }
        }
        return super.has(name, start);
    }

    @Override
    public void put(int index, Scriptable start, Object val) {
        js_set(index, val);
    }

    @Override
    public void put(String name, Scriptable start, Object val) {
        Optional<Double> num = ScriptRuntime.canonicalNumericIndexString(name);
        if (num.isPresent()) {
            int ix = toIndex(num.get());
            if (ix >= 0) {
                js_set(ix, val);
            }
        } else {
            super.put(name, start, val);
        }
    }

    @Override
    public void delete(int index) {}

    @Override
    public void delete(String name) {
        Optional<Double> num = ScriptRuntime.canonicalNumericIndexString(name);
        if (!num.isPresent()) {
            // No delete for indexed elements, so only delete if "name" is not a number
            super.delete(name);
        }
    }

    @Override
    public Object[] getIds() {
        Object[] ret = new Object[length];
        for (int i = 0; i < length; i++) {
            ret[i] = Integer.valueOf(i);
        }
        return ret;
    }

    @Override
    protected boolean defineOwnProperty(
            Context cx, Object id, ScriptableObject desc, boolean checkValid) {
        if (id instanceof CharSequence) {
            String name = id.toString();
            Optional<Double> num = ScriptRuntime.canonicalNumericIndexString(name);
            if (num.isPresent()) {
                int idx = num.get().intValue();
                if (checkIndex(idx)) {
                    return false;
                }

                if (Boolean.FALSE.equals(getProperty(desc, "configurable"))) {
                    return false;
                }
                if (Boolean.FALSE.equals(getProperty(desc, "enumerable"))) {
                    return false;
                }
                if (isAccessorDescriptor(desc)) {
                    return false;
                }
                if (Boolean.FALSE.equals(getProperty(desc, "writable"))) {
                    return false;
                }
                Object value = getProperty(desc, "value");
                if (value != NOT_FOUND) {
                    js_set(idx, value);
                }
                return true;
            }
        }
        return super.defineOwnProperty(cx, id, desc, checkValid);
    }

    /**
     * To aid in parsing: Return a positive (or zero) integer if the double is a valid array index,
     * and -1 if not.
     */
    private static int toIndex(double num) {
        int ix = (int) num;
        if (ix == num && ix >= 0) {
            return ix;
        }
        return -1;
    }

    // Actual functions

    static void init(
            Context cx, Scriptable scope, LambdaConstructor constructor, RealThis realThis) {
        constructor.definePrototypeProperty(
                cx,
                "buffer",
                (Scriptable thisObj) -> js_buffer(thisObj, realThis),
                DONTENUM | READONLY);
        constructor.definePrototypeProperty(
                cx,
                "byteLength",
                (Scriptable thisObj) -> js_byteLength(thisObj, realThis),
                DONTENUM | READONLY);
        constructor.definePrototypeProperty(
                cx,
                "byteOffset",
                (Scriptable thisObj) -> js_byteOffset(thisObj, realThis),
                DONTENUM | READONLY);
        constructor.definePrototypeProperty(
                cx,
                "length",
                (Scriptable thisObj) -> js_length(thisObj, realThis),
                DONTENUM | READONLY);

        constructor.definePrototypeMethod(
                scope,
                "at",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        js_at(lcx, lscope, thisObj, args, realThis),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "copyWithin",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        js_copyWithin(lcx, lscope, thisObj, args, realThis),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "entries",
                0,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
                    return new NativeArrayIterator(lscope, thisObj, ARRAY_ITERATOR_TYPE.ENTRIES);
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "every",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
                    return ArrayLikeAbstractOperations.iterativeMethod(
                            lcx, IterativeOperation.EVERY, lscope, thisObj, args);
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "fill",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        js_fill(lcx, lscope, thisObj, args, realThis),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "filter",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    var record = TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
                    Object array =
                            ArrayLikeAbstractOperations.iterativeMethod(
                                    lcx, IterativeOperation.FILTER, lscope, thisObj, args);
                    return record.object.typedArraySpeciesCreate(
                            lcx, lscope, new Object[] {array}, "filter");
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "find",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
                    return ArrayLikeAbstractOperations.iterativeMethod(
                            lcx, IterativeOperation.FIND, lscope, thisObj, args);
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "findIndex",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
                    return ArrayLikeAbstractOperations.iterativeMethod(
                            lcx, IterativeOperation.FIND_INDEX, lscope, thisObj, args);
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "findLast",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
                    return ArrayLikeAbstractOperations.iterativeMethod(
                            lcx, IterativeOperation.FIND_LAST, lscope, thisObj, args);
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "findLastIndex",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
                    return ArrayLikeAbstractOperations.iterativeMethod(
                            lcx, IterativeOperation.FIND_LAST_INDEX, lscope, thisObj, args);
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "forEach",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
                    return ArrayLikeAbstractOperations.iterativeMethod(
                            lcx, IterativeOperation.FOR_EACH, lscope, thisObj, args);
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "includes",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        js_includes(lcx, lscope, thisObj, args, realThis),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "indexOf",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        js_indexOf(lcx, lscope, thisObj, args, realThis),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "join",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        js_join(lcx, lscope, thisObj, args, realThis),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "keys",
                0,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
                    return new NativeArrayIterator(lscope, thisObj, ARRAY_ITERATOR_TYPE.KEYS);
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "lastIndexOf",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        js_lastIndexOf(lcx, lscope, thisObj, args, realThis),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "map",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    var record = TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
                    Object array =
                            ArrayLikeAbstractOperations.iterativeMethod(
                                    lcx, IterativeOperation.MAP, lscope, thisObj, args);
                    return record.object.typedArraySpeciesCreate(
                            lcx, lscope, new Object[] {array}, "map");
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "reduce",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
                    return ArrayLikeAbstractOperations.reduceMethod(
                            lcx, ReduceOperation.REDUCE, lscope, thisObj, args);
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "reduceRight",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
                    return ArrayLikeAbstractOperations.reduceMethod(
                            lcx, ReduceOperation.REDUCE_RIGHT, lscope, thisObj, args);
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "reverse",
                0,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        js_reverse(lcx, lscope, thisObj, args, realThis),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "set",
                0,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        js_set(lcx, lscope, thisObj, args, realThis),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "slice",
                2,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        js_slice(lcx, lscope, thisObj, args, realThis),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "some",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
                    return ArrayLikeAbstractOperations.iterativeMethod(
                            lcx, IterativeOperation.SOME, lscope, thisObj, args);
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "sort",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        js_sort(lcx, lscope, thisObj, args, realThis),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "subarray",
                2,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        js_subarray(lcx, lscope, thisObj, args, realThis),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "toLocaleString",
                0,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        js_toString(lcx, lscope, thisObj, args, realThis, true),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "toReversed",
                0,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        js_toReversed(lcx, lscope, thisObj, args, realThis),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "toSorted",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        js_toSorted(lcx, lscope, thisObj, args, realThis),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "toString",
                0,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        js_toString(lcx, lscope, thisObj, args, realThis, false),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "values",
                0,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
                    return new NativeArrayIterator(lscope, thisObj, ARRAY_ITERATOR_TYPE.VALUES);
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "with",
                2,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        js_with(lcx, lscope, thisObj, args, realThis),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                SymbolKey.ITERATOR,
                0,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
                    return new NativeArrayIterator(lscope, thisObj, ARRAY_ITERATOR_TYPE.VALUES);
                },
                DONTENUM,
                DONTENUM | READONLY);
    }

    /** Returns <code>true</code>, if the index is wrong. */
    protected boolean checkIndex(int index) {
        if (arrayBuffer.isDetached()) {
            return true;
        }
        if (index < 0) {
            return true;
        }
        var record = TypedArrayBufferWitnessRecord.create(this);
        if (record.isTypedArrayOutOfBounds()) {
            return true;
        }

        return index >= record.getTypedArrayLength();
    }

    /**
     * Enusres that the index is in the given range
     *
     * @throws IndexOutOfBoundsException when index is out of range
     */
    protected void ensureIndex(int index) {
        if (checkIndex(index)) {
            throw new IndexOutOfBoundsException("Index: " + index + ", length: " + length);
        }
    }

    /**
     * Return the number of bytes represented by each element in the array. This can be useful when
     * wishing to manipulate the byte array directly from Java.
     */
    public abstract int getBytesPerElement();

    protected abstract Object js_get(int index);

    protected abstract Object js_set(int index, Object c);

    private static NativeArrayBuffer makeArrayBuffer(
            Context cx, Scriptable scope, int length, int bytesPerElement) {
        return (NativeArrayBuffer)
                cx.newObject(
                        scope,
                        NativeArrayBuffer.CLASS_NAME,
                        new Object[] {Double.valueOf((double) length * bytesPerElement)});
    }

    protected interface TypedArrayConstructable {
        NativeTypedArrayView<?> construct(NativeArrayBuffer ab, int off, int len);
    }

    protected interface RealThis {
        NativeTypedArrayView<?> realThis(Scriptable thisObj);
    }

    protected static NativeTypedArrayView<?> js_constructor(
            Context cx,
            Scriptable scope,
            Object[] args,
            TypedArrayConstructable constructable,
            int bytesPerElement) {
        if (!isArg(args, 0)) {
            return constructable.construct(new NativeArrayBuffer(), 0, 0);
        }

        final Object arg0 = args[0];
        if (arg0 == null) {
            return constructable.construct(new NativeArrayBuffer(), 0, 0);
        }

        if ((arg0 instanceof Number) || (arg0 instanceof String)) {
            // Create a zeroed-out array of a certain length
            int length = ScriptRuntime.toInt32(arg0);
            NativeArrayBuffer buffer = makeArrayBuffer(cx, scope, length, bytesPerElement);
            return constructable.construct(buffer, 0, length);
        }

        if (arg0 instanceof NativeTypedArrayView) {
            // Copy elements from the old array and convert them into our own
            NativeTypedArrayView<?> src = (NativeTypedArrayView<?>) arg0;
            NativeArrayBuffer na = makeArrayBuffer(cx, scope, src.length, bytesPerElement);
            NativeTypedArrayView<?> v = constructable.construct(na, 0, src.length);

            for (int i = 0; i < src.length; i++) {
                v.js_set(i, src.js_get(i));
            }
            return v;
        }

        if (arg0 instanceof NativeArrayBuffer) {
            // Make a slice of an existing buffer, with shared storage
            NativeArrayBuffer na = (NativeArrayBuffer) arg0;
            int byteOff = isArg(args, 1) ? ScriptRuntime.toIndex(args[1]) : 0;

            if ((byteOff % bytesPerElement) != 0) {
                throw ScriptRuntime.rangeErrorById(
                        "msg.typed.array.bad.offset.byte.size", byteOff, bytesPerElement);
            }

            int newLength = 0;
            if (isArg(args, 2)) {
                newLength = ScriptRuntime.toIndex(args[2]);
            }

            if (na.isDetached()) {
                throw ScriptRuntime.typeErrorById("msg.arraybuf.detached");
            }
            int bufferByteLength = na.getLength();

            int newByteLength;
            if (!isArg(args, 2)) {
                if ((bufferByteLength % bytesPerElement) != 0) {
                    throw ScriptRuntime.rangeErrorById(
                            "msg.typed.array.bad.buffer.length.byte.size",
                            bufferByteLength,
                            bytesPerElement);
                }
                newByteLength = bufferByteLength - byteOff;
                if (newByteLength < 0) {
                    throw ScriptRuntime.rangeErrorById("msg.typed.array.bad.length", newByteLength);
                }
            } else {
                newByteLength = newLength * bytesPerElement;

                if (byteOff + newByteLength > bufferByteLength) {
                    throw ScriptRuntime.rangeErrorById("msg.typed.array.bad.offset", byteOff);
                }
            }

            if ((byteOff < 0) || (byteOff > na.getLength())) {
                throw ScriptRuntime.rangeErrorById("msg.typed.array.bad.offset", byteOff);
            }

            return constructable.construct(na, byteOff, newByteLength / bytesPerElement);
        }

        if (arg0 instanceof NativeArray) {
            // Copy elements of the array and convert them to the correct type
            NativeArray array = (NativeArray) arg0;

            NativeArrayBuffer na = makeArrayBuffer(cx, scope, array.size(), bytesPerElement);
            NativeTypedArrayView<?> v = constructable.construct(na, 0, array.size());
            for (int i = 0; i < array.size(); i++) {
                // we have to call this here to get the raw value;
                // null has to be forewoded as null
                final Object value = array.get(i, array);
                if (value == Scriptable.NOT_FOUND || value == Undefined.instance) {
                    v.js_set(i, ScriptRuntime.NaNobj);
                } else if (value instanceof Wrapper) {
                    v.js_set(i, ((Wrapper) value).unwrap());
                } else {
                    v.js_set(i, value);
                }
            }
            return v;
        }

        if (ScriptRuntime.isArrayObject(arg0)) {
            // Copy elements of the array and convert them to the correct type
            Object[] arrayElements = ScriptRuntime.getArrayElements((Scriptable) arg0);

            NativeArrayBuffer na =
                    makeArrayBuffer(cx, scope, arrayElements.length, bytesPerElement);
            NativeTypedArrayView<?> v = constructable.construct(na, 0, arrayElements.length);
            for (int i = 0; i < arrayElements.length; i++) {
                v.js_set(i, arrayElements[i]);
            }
            return v;
        }
        throw ScriptRuntime.constructError("Error", "invalid argument");
    }

    private void setRange(NativeTypedArrayView<?> v, int off) {
        var targetRecord = TypedArrayBufferWitnessRecord.create(this);
        if (targetRecord.isTypedArrayOutOfBounds()) {
            throw ScriptRuntime.typeError("typed array out of bounds");
        }
        int targetLength = targetRecord.getTypedArrayLength();

        var srcRecord = TypedArrayBufferWitnessRecord.create(v);
        if (srcRecord.isTypedArrayOutOfBounds()) {
            throw ScriptRuntime.typeError("typed array out of bounds");
        }
        int srcLength = srcRecord.getTypedArrayLength();


        if (srcLength + off > targetLength) {
            throw ScriptRuntime.rangeErrorById("msg.typed.array.bad.source.array");
        }

        if (v.arrayBuffer == arrayBuffer) {
            // Copy to temporary space first, as per spec, to avoid messing up overlapping copies
            Object[] tmp = new Object[srcLength];
            for (int i = 0; i < srcLength; i++) {
                tmp[i] = v.js_get(i);
            }
            for (int i = 0; i < srcLength; i++) {
                js_set(i + off, tmp[i]);
            }
        } else {
            for (int i = 0; i < srcLength; i++) {
                js_set(i + off, v.js_get(i));
            }
        }
    }

    private void setRange(Scriptable a, int off) {
        var targetRecord = TypedArrayBufferWitnessRecord.create(this);
        if (targetRecord.isTypedArrayOutOfBounds()) {
            throw ScriptRuntime.typeError("typed array out of bounds");
        }
        int targetLength = targetRecord.getTypedArrayLength();

        ScriptableObject src = ScriptableObject.ensureScriptableObject(a);
        long srcLength = ScriptRuntime.toLength(src.get("length", src));

        if (srcLength + off > targetLength) {
            throw ScriptRuntime.rangeErrorById("msg.typed.array.bad.source.array");
        }

        for (int k = 0; k < srcLength; k++) {
            Object value = src.get(k, src);
            js_set(k + off, value);
        }
    }

    private static Object js_buffer(Scriptable thisObj, RealThis realThis) {
        return realThis.realThis(thisObj).arrayBuffer;
    }

    private static Object js_byteLength(Scriptable thisObj, RealThis realThis) {
        NativeTypedArrayView<?> o = realThis.realThis(thisObj);
        var record = TypedArrayBufferWitnessRecord.create(o);
        return record.getTypedArrayByteLength();
    }

    private static Object js_byteOffset(Scriptable thisObj, RealThis realThis) {
        NativeTypedArrayView<?> o = realThis.realThis(thisObj);
        var record = TypedArrayBufferWitnessRecord.create(o);
        if (record.isTypedArrayOutOfBounds()) {
            return 0;
        }
        return o.getByteOffset();
    }

    private static Object js_length(Scriptable thisObj, RealThis realThis) {
        NativeTypedArrayView<?> o = realThis.realThis(thisObj);
        var record = TypedArrayBufferWitnessRecord.create(o);
        if (record.isTypedArrayOutOfBounds()) {
            return 0;
        }
        return record.getTypedArrayLength();
    }

    private static String js_toString(
            Context cx,
            Scriptable scope,
            Scriptable thisObj,
            Object[] args,
            RealThis realThis,
            boolean useLocale) {
        NativeTypedArrayView<?> self = realThis.realThis(thisObj);
        StringBuilder builder = new StringBuilder();
        if (self.length > 0) {
            Object elem = self.getElemForToString(cx, scope, 0, useLocale);
            builder.append(ScriptRuntime.toString(elem));
        }
        for (int i = 1; i < self.length; i++) {
            builder.append(',');
            Object elem = self.getElemForToString(cx, scope, i, useLocale);
            builder.append(ScriptRuntime.toString(elem));
        }
        return builder.toString();
    }

    private Object getElemForToString(Context cx, Scriptable scope, int index, boolean useLocale) {
        var elem = js_get(index);
        if (useLocale) {
            Callable fun = ScriptRuntime.getPropFunctionAndThis(elem, "toLocaleString", cx, scope);
            Scriptable funThis = ScriptRuntime.lastStoredScriptable(cx);
            return fun.call(cx, scope, funThis, ScriptRuntime.emptyArgs);
        } else {
            return elem;
        }
    }

    private static Boolean js_includes(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args, RealThis realThis) {
        var record = TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
        int len = record.getTypedArrayLength();

        if (len == 0) return Boolean.FALSE;

        Object compareTo = args.length > 0 ? args[0] : Undefined.instance;

        long start;
        if (args.length < 2) {
            start = 0;
        } else {
            start = (long) ScriptRuntime.toInteger(args[1]);
            if (start < 0) {
                start += len;
                if (start < 0) start = 0;
            }
            if (start > len - 1) return Boolean.FALSE;
        }
        for (int i = (int) start; i < len; i++) {
            Object val = record.object.js_get(i);
            if (ScriptRuntime.sameZero(val, compareTo)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    private static Object js_indexOf(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args, RealThis realThis) {
        var record = TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
        int len = record.getTypedArrayLength();

        if (len == 0) return -1;

        Object compareTo = args.length > 0 ? args[0] : Undefined.instance;

        long start;
        if (args.length < 2) {
            // default
            start = 0;
        } else {
            start = (long) ScriptRuntime.toInteger(args[1]);
            if (start < 0) {
                start += len;
                if (start < 0) start = 0;
            }
            if (start > len - 1) return -1;
        }
        for (int i = (int) start; i < len; i++) {
            // TODO: if looking for undefined and the buffer is detached, it shouldn't find it.
            //       need to investigate more
            Object val = record.object.js_get(i);
            if (val != NOT_FOUND && ScriptRuntime.shallowEq(val, compareTo)) {
                return (long) i;
            }
        }
        return -1;
    }

    private static Object js_lastIndexOf(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args, RealThis realThis) {
        var record = TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
        int len = record.getTypedArrayLength();

        if (len == 0) return -1;

        Object compareTo = args.length > 0 ? args[0] : Undefined.instance;

        long start;
        if (args.length < 2) {
            // default
            start = len - 1L;
        } else {
            start = (long) ScriptRuntime.toInteger(args[1]);
            if (start >= len) start = len - 1L;
            else if (start < 0) start += len;
            if (start < 0) return -1;
        }
        for (int i = (int) start; i >= 0; i--) {
            Object val = record.object.js_get(i);
            if (val != NOT_FOUND && ScriptRuntime.shallowEq(val, compareTo)) {
                return (long) i;
            }
        }
        return -1;
    }

    private static Scriptable js_slice(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args, RealThis realThis) {
        NativeTypedArrayView<?> self = realThis.realThis(thisObj);
        var record = TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
        int srcLength = record.getTypedArrayLength();

        long begin, end;
        if (args.length == 0) {
            begin = 0;
            end = srcLength;
        } else {
            begin =
                    ArrayLikeAbstractOperations.toSliceIndex(
                            ScriptRuntime.toInteger(args[0]), srcLength);
            if (args.length == 1 || args[1] == Undefined.instance) {
                end = srcLength;
            } else {
                end =
                        ArrayLikeAbstractOperations.toSliceIndex(
                                ScriptRuntime.toInteger(args[1]), srcLength);
            }
        }

        if (end - begin > Integer.MAX_VALUE) {
            String msg = ScriptRuntime.getMessageById("msg.arraylength.bad");
            throw ScriptRuntime.rangeError(msg);
        }

        long count = Math.max(end - begin, 0);

        var a = (NativeTypedArrayView<?>) record.object.typedArraySpeciesCreate(
                cx, scope, new Object[] {count}, "slice");

        if (count > 0) {
            record = TypedArrayBufferWitnessRecord.create(self);
            if (record.isTypedArrayOutOfBounds()) {
                throw ScriptRuntime.typeError("typed array out of bounds");
            }
            end = Math.min(end, record.getTypedArrayLength());

            // TODO: fix impl to be spec compliant with different types
            int otherIndex = 0;
            for (long i = begin; i < end; i++) {
                Object val = self.js_get((int) i);
                a.js_set(otherIndex, val);
                otherIndex++;
            }
        }

        return a;
    }

    private static String js_join(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args, RealThis realThis) {
        var record = TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
        int len = record.getTypedArrayLength();

        // if no args, use "," as separator
        String separator =
                (args.length < 1 || args[0] == Undefined.instance)
                        ? ","
                        : ScriptRuntime.toString(args[0]);
        if (len == 0) {
            return "";
        }

        String[] buf = new String[len];
        int total_size = 0;
        for (int i = 0; i != len; i++) {
            Object temp = record.object.js_get(i);
            if (temp != null && temp != Undefined.instance) {
                String str = ScriptRuntime.toString(temp);
                total_size += str.length();
                buf[i] = str;
            }
        }
        total_size += (len - 1) * separator.length();
        StringBuilder sb = new StringBuilder(total_size);
        for (int i = 0; i != len; i++) {
            if (i != 0) {
                sb.append(separator);
            }
            String str = buf[i];
            if (str != null) {
                // str == null for undefined or null
                sb.append(str);
            }
        }
        return sb.toString();
    }

    private static NativeTypedArrayView<?> js_reverse(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args, RealThis realThis) {
        var record = TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
        int len = record.getTypedArrayLength();

        for (int i = 0, j = len - 1; i < j; i++, j--) {
            Object temp = record.object.js_get(i);
            record.object.js_set(i, record.object.js_get(j));
            record.object.js_set(j, temp);
        }
        return record.object;
    }

    private static NativeTypedArrayView<?> js_fill(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args, RealThis realThis) {
        NativeTypedArrayView<?> self = realThis.realThis(thisObj);
        var record = TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
        int len = record.getTypedArrayLength();

        Object value = ScriptRuntime.toNumber(isArg(args, 0) ? args[0] : Undefined.instance);

        long relativeStart = 0;
        if (args.length >= 2) {
            relativeStart = (long) ScriptRuntime.toInteger(args[1]);
        }
        final long k;
        if (relativeStart < 0) {
            k = Math.max((len + relativeStart), 0);
        } else {
            k = Math.min(relativeStart, len);
        }

        long relativeEnd = len;
        if (isArg(args, 2)) {
            relativeEnd = (long) ScriptRuntime.toInteger(args[2]);
        }
        long fin;
        if (relativeEnd < 0) {
            fin = Math.max((len + relativeEnd), 0);
        } else {
            fin = Math.min(relativeEnd, len);
        }

        record = TypedArrayBufferWitnessRecord.create(self);
        if (record.isTypedArrayOutOfBounds()) {
            throw ScriptRuntime.typeError("typed array out of bounds");
        }
        len = record.getTypedArrayLength();

        fin = Math.min(len, fin);

        for (int i = (int) k; i < fin; i++) {
            self.js_set(i, value);
        }

        return self;
    }

    private static Scriptable js_sort(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args, RealThis realThis) {
        if (isArg(args, 0) && !(args[0] instanceof Callable)) {
            throw ScriptRuntime.typeErrorById("msg.function.expected");
        }

        var record = TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
        int len = record.getTypedArrayLength();

        Object[] working = record.object.sortTemporaryArray(cx, scope, args);
        for (int i = 0; i < len; ++i) {
            record.object.js_set(i, working[i]);
        }

        return record.object;
    }

    private Object[] sortTemporaryArray(Context cx, Scriptable scope, Object[] args) {
        Comparator<Object> comparator;
        if (args.length > 0 && Undefined.instance != args[0]) {
            comparator = ArrayLikeAbstractOperations.getSortComparator(cx, scope, args);
        } else {
            comparator = Comparator.comparingDouble(e -> ((Number) e).doubleValue());
        }

        // Temporary array to rely on Java's built-in sort, which is stable.
        Object[] working = toArray();
        Arrays.sort(working, comparator);
        return working;
    }

    private static Object js_copyWithin(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args, RealThis realThis) {
        var record = TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
        int len = record.getTypedArrayLength();

        Object targetArg = (args.length >= 1) ? args[0] : Undefined.instance;
        long relativeTarget = (long) ScriptRuntime.toInteger(targetArg);
        long to;
        if (relativeTarget < 0) {
            to = Math.max((len + relativeTarget), 0);
        } else {
            to = Math.min(relativeTarget, len);
        }

        Object startArg = (args.length >= 2) ? args[1] : Undefined.instance;
        long relativeStart = (long) ScriptRuntime.toInteger(startArg);
        long from;
        if (relativeStart < 0) {
            from = Math.max((len + relativeStart), 0);
        } else {
            from = Math.min(relativeStart, len);
        }

        long relativeEnd = len;
        if (isArg(args, 2)) {
            relativeEnd = (long) ScriptRuntime.toInteger(args[2]);
        }
        final long fin;
        if (relativeEnd < 0) {
            fin = Math.max((len + relativeEnd), 0);
        } else {
            fin = Math.min(relativeEnd, len);
        }

        long count = Math.min(fin - from, len - to);

        record = TypedArrayBufferWitnessRecord.create(realThis.realThis(thisObj));
        if (record.isTypedArrayOutOfBounds()) {
            throw ScriptRuntime.typeError("typed array out of bounds");
        }

        int direction = 1;
        if (from < to && to < from + count) {
            direction = -1;
            from = from + count - 1;
            to = to + count - 1;
        }

        for (; count > 0; count--) {
            final Object temp = record.object.js_get((int) from);
            record.object.js_set((int) to, temp);
            from += direction;
            to += direction;
        }

        return record.object;
    }

    private static Object js_set(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args, RealThis realThis) {
        NativeTypedArrayView<?> self = realThis.realThis(thisObj);
        int offset = isArg(args, 1) ? ScriptRuntime.toInt32(args[1]) : 0;
        if (offset < 0) {
            throw ScriptRuntime.rangeErrorById("msg.typed.array.bad.offset", offset);
        }

        if (args[0] instanceof NativeTypedArrayView) {
            NativeTypedArrayView<?> source = (NativeTypedArrayView<?>) args[0];
            self.setRange(source, offset);
        } else {
            self.setRange(ScriptableObject.ensureScriptable(args[0]), offset);
        }
        return Undefined.instance;
    }

    private static Object js_subarray(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args, RealThis realThis) {
        NativeTypedArrayView<?> self = realThis.realThis(thisObj);
        var record = TypedArrayBufferWitnessRecord.create(self);
        int srcLength = record.isTypedArrayOutOfBounds() ? 0 : record.getTypedArrayLength();

        int start = isArg(args, 0) ? ScriptRuntime.toInt32(args[0]) : 0;
        int end = isArg(args, 1) ? ScriptRuntime.toInt32(args[1]) : srcLength;
        if (cx.getLanguageVersion() >= Context.VERSION_ES6 || args.length > 0) {
            start = (start < 0 ? srcLength + start : start);
            end = (end < 0 ? srcLength + end : end);

            // Clamping behavior as described by the spec.
            start = Math.max(0, start);
            end = Math.min(srcLength, end);
            int len = Math.max(0, (end - start));
            int byteOff =
                    Math.min(
                            self.getByteOffset() + start * self.getBytesPerElement(),
                            self.arrayBuffer.getLength());

            return cx.newObject(
                    scope, self.getClassName(), new Object[] {self.arrayBuffer, byteOff, len});
        }
        throw ScriptRuntime.constructError("Error", "invalid arguments");
    }

    private static Object js_at(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args, RealThis realThis) {
        var record = TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
        int len = record.getTypedArrayLength();

        long relativeIndex = 0;
        if (args.length >= 1) {
            relativeIndex = (long) ScriptRuntime.toInteger(args[0]);
        }

        long k = (relativeIndex >= 0) ? relativeIndex : len + relativeIndex;

        if ((k < 0) || (k >= len)) {
            return Undefined.instance;
        }

        return getProperty(thisObj, (int) k);
    }

    private Scriptable typedArraySpeciesCreate(
            Context cx, Scriptable scope, Object[] args, String methodName) {
        Scriptable topLevelScope = ScriptableObject.getTopLevelScope(scope);
        Function defaultConstructor =
                ScriptRuntime.getExistingCtor(cx, topLevelScope, getClassName());
        Constructable constructable =
                AbstractEcmaObjectOperations.speciesConstructor(cx, this, defaultConstructor);

        Scriptable newArray = constructable.construct(cx, scope, args);
        if (!(newArray instanceof NativeTypedArrayView<?>)) {
            throw ScriptRuntime.typeErrorById("msg.typed.array.ctor.incompatible", methodName);
        }
        var record = TypedArrayBufferWitnessRecord.validateTypedArray(newArray, null);
        if (args.length == 1) {
            Object arg0 = ScriptRuntime.toPrimitive(args[0]);
            if (arg0 instanceof Number) {
                if (record.isTypedArrayOutOfBounds()) {
                    throw ScriptRuntime.typeError("typed array out of bounds");
                }
                int length = record.getTypedArrayLength();
                if (length < ScriptRuntime.toNumber(arg0)) {
                    throw ScriptRuntime.typeError("bad length");
                }
            }
        }
        return newArray;
    }

    private static Object js_toReversed(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args, RealThis realThis) {
        NativeTypedArrayView<?> self = realThis.realThis(thisObj);
        var record = TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
        int len = record.getTypedArrayLength();

        NativeArrayBuffer newBuffer =
                new NativeArrayBuffer(len * self.getBytesPerElement());
        Scriptable result =
                cx.newObject(
                        scope,
                        self.getClassName(),
                        new Object[] {newBuffer, 0, len, self.getBytesPerElement()});

        for (int k = 0; k < len; ++k) {
            int from = len - k - 1;
            Object fromValue = self.js_get(from);
            result.put(k, result, fromValue);
        }

        return result;
    }

    private static Object js_toSorted(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args, RealThis realThis) {
        NativeTypedArrayView<?> self = realThis.realThis(thisObj);
        var record = TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
        int len = record.getTypedArrayLength();

        Object[] working = self.sortTemporaryArray(cx, scope, args);

        // Move value in a new typed array of the same type
        NativeArrayBuffer newBuffer =
                new NativeArrayBuffer(len * self.getBytesPerElement());
        Scriptable result =
                cx.newObject(
                        scope,
                        self.getClassName(),
                        new Object[] {newBuffer, 0, len, self.getBytesPerElement()});
        for (int k = 0; k < len; ++k) {
            result.put(k, result, working[k]);
        }

        return result;
    }

    private static Object js_with(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args, RealThis realThis) {
        var record = TypedArrayBufferWitnessRecord.validateTypedArray(thisObj, realThis);
        int len = record.getTypedArrayLength();

        long relativeIndex = args.length > 0 ? (int) ScriptRuntime.toInteger(args[0]) : 0;
        long actualIndex = relativeIndex >= 0 ? relativeIndex : len + relativeIndex;

        Object argsValue = args.length > 1 ? ScriptRuntime.toNumber(args[1]) : 0.0;

        if (actualIndex < 0 || actualIndex >= len) {
            String msg =
                    ScriptRuntime.getMessageById(
                            "msg.typed.array.index.out.of.bounds",
                            relativeIndex,
                            len * -1,
                            len - 1);
            throw ScriptRuntime.rangeError(msg);
        }

        NativeArrayBuffer newBuffer =
                new NativeArrayBuffer(len * record.object.getBytesPerElement());
        Scriptable result =
                cx.newObject(
                        scope,
                        record.object.getClassName(),
                        new Object[] {newBuffer, 0, len, record.object.getBytesPerElement()});

        for (int k = 0; k < len; ++k) {
            Object fromValue = (k == actualIndex) ? argsValue : record.object.js_get(k);
            result.put(k, result, fromValue);
        }

        return result;
    }

    private static class TypedArrayBufferWitnessRecord {
        private final NativeTypedArrayView<?> object;
        private final int cachedBufferByteLength;
        private static final int DETACHED = -1;

        private TypedArrayBufferWitnessRecord(NativeTypedArrayView<?> object, int cachedBufferByteLength) {
            this.object = object;
            this.cachedBufferByteLength = cachedBufferByteLength;
        }

        static TypedArrayBufferWitnessRecord create(NativeTypedArrayView<?> object) {
            var buffer = object.arrayBuffer;
            return new TypedArrayBufferWitnessRecord(
                    object,
                    buffer.isDetached() ? DETACHED : buffer.getLength()
            );
        }

        static TypedArrayBufferWitnessRecord validateTypedArray(Scriptable object, RealThis realThis) {
            if (realThis != null) {
                var o = realThis.realThis(object);
                var record = create(o);
                if (record.isTypedArrayOutOfBounds()) {
                    throw ScriptRuntime.typeError("typed array out of bounds");
                }
                return record;
            }

            if (object instanceof NativeTypedArrayView<?>) {
                var o = (NativeTypedArrayView<?>) object;
                var record = create(o);
                if (record.isTypedArrayOutOfBounds()) {
                    throw ScriptRuntime.typeError("typed array out of bounds");
                }
                return record;
            }

            throw ScriptRuntime.typeError("Expected a TypedArray instance");
        }

        public int getTypedArrayByteLength() {
            if (isTypedArrayOutOfBounds()) {
                return 0;
            }
            int length = getTypedArrayLength();
            if (length == 0) {
                return 0;
            }
            return object.byteLength;
        }

        public int getTypedArrayLength() {
            return object.getArrayLength();
        }

        public boolean isTypedArrayOutOfBounds() {
            var ta = object;
            int bufferByteLength = cachedBufferByteLength;
            if (bufferByteLength == DETACHED) {
                return true;
            }

            int byteOffsetStart = ta.offset;
            int byteOffsetEnd = byteOffsetStart + ta.byteLength;

            return byteOffsetStart > bufferByteLength || byteOffsetEnd > bufferByteLength;
        }
    }

    // External Array implementation

    @Override
    public Object getArrayElement(int index) {
        return js_get(index);
    }

    @Override
    public void setArrayElement(int index, Object value) {
        js_set(index, value);
    }

    @Override
    public int getArrayLength() {
        return length;
    }

    // Abstract List implementation

    @SuppressWarnings("unused")
    @Override
    public boolean containsAll(Collection<?> objects) {
        for (Object o : objects) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unused")
    @Override
    public int indexOf(Object o) {
        for (int i = 0; i < length; i++) {
            if (o.equals(js_get(i))) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings("unused")
    @Override
    public int lastIndexOf(Object o) {
        for (int i = length - 1; i >= 0; i--) {
            if (o.equals(js_get(i))) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings("unused")
    @Override
    public Object[] toArray() {
        Object[] a = new Object[length];
        for (int i = 0; i < length; i++) {
            a[i] = js_get(i);
        }
        return a;
    }

    @SuppressWarnings({"unused", "unchecked"})
    @Override
    public <U> U[] toArray(U[] ts) {
        U[] a;

        if (ts.length >= length) {
            a = ts;
        } else {
            a = (U[]) Array.newInstance(ts.getClass().getComponentType(), length);
        }

        for (int i = 0; i < length; i++) {
            try {
                a[i] = (U) js_get(i);
            } catch (ClassCastException cce) {
                throw new ArrayStoreException();
            }
        }
        return a;
    }

    @SuppressWarnings("unused")
    @Override
    public int size() {
        return length;
    }

    @SuppressWarnings("unused")
    @Override
    public boolean isEmpty() {
        return (length == 0);
    }

    @SuppressWarnings("unused")
    @Override
    public boolean contains(Object o) {
        return (indexOf(o) >= 0);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof NativeTypedArrayView)) {
            return false;
        }
        NativeTypedArrayView<T> v = (NativeTypedArrayView<T>) o;
        if (length != v.length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (!js_get(i).equals(v.js_get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hc = 0;
        for (int i = 0; i < length; i++) {
            hc += js_get(i).hashCode();
        }
        return hc;
    }

    @SuppressWarnings("unused")
    @Override
    public Iterator<T> iterator() {
        return new NativeTypedArrayIterator<>(this, 0);
    }

    @SuppressWarnings("unused")
    @Override
    public ListIterator<T> listIterator() {
        return new NativeTypedArrayIterator<>(this, 0);
    }

    @SuppressWarnings("unused")
    @Override
    public ListIterator<T> listIterator(int start) {
        ensureIndex(start);
        return new NativeTypedArrayIterator<>(this, start);
    }

    @SuppressWarnings("unused")
    @Override
    public List<T> subList(int i, int i2) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    @Override
    public boolean add(T aByte) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    @Override
    public void add(int i, T aByte) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    @Override
    public boolean addAll(Collection<? extends T> bytes) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    @Override
    public boolean addAll(int i, Collection<? extends T> bytes) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    @Override
    public T remove(int i) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    @Override
    public boolean removeAll(Collection<?> objects) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    @Override
    public boolean retainAll(Collection<?> objects) {
        throw new UnsupportedOperationException();
    }
}
