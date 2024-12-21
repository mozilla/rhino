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
                    NativeTypedArrayView<?> self = realThis.realThis(thisObj);
                    return new NativeArrayIterator(lscope, self, ARRAY_ITERATOR_TYPE.ENTRIES);
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "every",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    NativeTypedArrayView<?> self = realThis.realThis(thisObj);
                    return ArrayLikeAbstractOperations.iterativeMethod(
                            lcx, IterativeOperation.EVERY, lscope, self, args);
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
                    NativeTypedArrayView<?> self = realThis.realThis(thisObj);
                    Object array =
                            ArrayLikeAbstractOperations.iterativeMethod(
                                    lcx, IterativeOperation.FILTER, lscope, self, args);
                    return self.typedArraySpeciesCreate(
                            lcx, lscope, new Object[] {array}, "filter");
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "find",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    NativeTypedArrayView<?> self = realThis.realThis(thisObj);
                    return ArrayLikeAbstractOperations.iterativeMethod(
                            lcx, IterativeOperation.FIND, lscope, self, args);
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "findIndex",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    NativeTypedArrayView<?> self = realThis.realThis(thisObj);
                    return ArrayLikeAbstractOperations.iterativeMethod(
                            lcx, IterativeOperation.FIND_INDEX, lscope, self, args);
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "findLast",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    NativeTypedArrayView<?> self = realThis.realThis(thisObj);
                    return ArrayLikeAbstractOperations.iterativeMethod(
                            lcx, IterativeOperation.FIND_LAST, lscope, self, args);
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "findLastIndex",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    NativeTypedArrayView<?> self = realThis.realThis(thisObj);
                    return ArrayLikeAbstractOperations.iterativeMethod(
                            lcx, IterativeOperation.FIND_LAST_INDEX, lscope, self, args);
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "forEach",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    NativeTypedArrayView<?> self = realThis.realThis(thisObj);
                    return ArrayLikeAbstractOperations.iterativeMethod(
                            lcx, IterativeOperation.FOR_EACH, lscope, self, args);
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
                    NativeTypedArrayView<?> self = realThis.realThis(thisObj);
                    return new NativeArrayIterator(lscope, self, ARRAY_ITERATOR_TYPE.KEYS);
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
                    NativeTypedArrayView<?> self = realThis.realThis(thisObj);
                    Object array =
                            ArrayLikeAbstractOperations.iterativeMethod(
                                    lcx, IterativeOperation.MAP, lscope, thisObj, args);
                    return self.typedArraySpeciesCreate(lcx, lscope, new Object[] {array}, "map");
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "reduce",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    NativeTypedArrayView<?> self = realThis.realThis(thisObj);
                    return ArrayLikeAbstractOperations.reduceMethod(
                            lcx, ReduceOperation.REDUCE, lscope, self, args);
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "reduceRight",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    NativeTypedArrayView<?> self = realThis.realThis(thisObj);
                    return ArrayLikeAbstractOperations.reduceMethod(
                            lcx, ReduceOperation.REDUCE_RIGHT, lscope, self, args);
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
                    NativeTypedArrayView<?> self = realThis.realThis(thisObj);
                    return ArrayLikeAbstractOperations.iterativeMethod(
                            lcx, IterativeOperation.SOME, lscope, self, args);
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
                    NativeTypedArrayView<?> self = realThis.realThis(thisObj);
                    return new NativeArrayIterator(lscope, self, ARRAY_ITERATOR_TYPE.VALUES);
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
                    NativeTypedArrayView<?> self = realThis.realThis(thisObj);
                    return new NativeArrayIterator(lscope, self, ARRAY_ITERATOR_TYPE.VALUES);
                },
                DONTENUM,
                DONTENUM | READONLY);
    }

    protected boolean checkIndex(int index) {
        return ((index < 0) || (index >= length));
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
            int byteOff = isArg(args, 1) ? ScriptRuntime.toInt32(args[1]) : 0;

            int byteLen;
            if (isArg(args, 2)) {
                byteLen = ScriptRuntime.toInt32(args[2]) * bytesPerElement;
            } else {
                byteLen = na.getLength() - byteOff;
            }

            if ((byteOff < 0) || (byteOff > na.getLength())) {
                String msg = ScriptRuntime.getMessageById("msg.typed.array.bad.offset", byteOff);
                throw ScriptRuntime.rangeError(msg);
            }
            if ((byteLen < 0) || ((byteOff + byteLen) > na.getLength())) {
                String msg = ScriptRuntime.getMessageById("msg.typed.array.bad.length", byteLen);
                throw ScriptRuntime.rangeError(msg);
            }
            if ((byteOff % bytesPerElement) != 0) {
                String msg =
                        ScriptRuntime.getMessageById(
                                "msg.typed.array.bad.offset.byte.size", byteOff, bytesPerElement);
                throw ScriptRuntime.rangeError(msg);
            }
            if ((byteLen % bytesPerElement) != 0) {
                String msg =
                        ScriptRuntime.getMessageById(
                                "msg.typed.array.bad.buffer.length.byte.size",
                                byteLen,
                                bytesPerElement);
                throw ScriptRuntime.rangeError(msg);
            }

            return constructable.construct(na, byteOff, byteLen / bytesPerElement);
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
        if (off < 0 || off > length) {
            String msg = ScriptRuntime.getMessageById("msg.typed.array.bad.offset", off);
            throw ScriptRuntime.rangeError(msg);
        }

        if (v.length > (length - off)) {
            String msg = ScriptRuntime.getMessageById("msg.typed.array.bad.source.array");
            throw ScriptRuntime.rangeError(msg);
        }

        if (v.arrayBuffer == arrayBuffer) {
            // Copy to temporary space first, as per spec, to avoid messing up overlapping copies
            Object[] tmp = new Object[v.length];
            for (int i = 0; i < v.length; i++) {
                tmp[i] = v.js_get(i);
            }
            for (int i = 0; i < v.length; i++) {
                js_set(i + off, tmp[i]);
            }
        } else {
            for (int i = 0; i < v.length; i++) {
                js_set(i + off, v.js_get(i));
            }
        }
    }

    private void setRange(NativeArray a, int off) {
        if (off < 0 || off > length) {
            String msg = ScriptRuntime.getMessageById("msg.typed.array.bad.offset", off);
            throw ScriptRuntime.rangeError(msg);
        }
        if ((off + a.size()) > length) {
            String msg = ScriptRuntime.getMessageById("msg.typed.array.bad.source.array");
            throw ScriptRuntime.rangeError(msg);
        }

        int pos = off;
        for (Object val : a) {
            js_set(pos, val);
            pos++;
        }
    }

    private static Object js_buffer(Scriptable thisObj, RealThis realThis) {
        return realThis.realThis(thisObj).arrayBuffer;
    }

    private static Object js_byteLength(Scriptable thisObj, RealThis realThis) {
        NativeTypedArrayView<?> o = realThis.realThis(thisObj);
        return o.byteLength;
    }

    private static Object js_byteOffset(Scriptable thisObj, RealThis realThis) {
        NativeTypedArrayView<?> o = realThis.realThis(thisObj);
        return o.offset;
    }

    private static Object js_length(Scriptable thisObj, RealThis realThis) {
        NativeTypedArrayView<?> o = realThis.realThis(thisObj);
        return o.length;
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
        NativeTypedArrayView<?> self = realThis.realThis(thisObj);
        Object compareTo = args.length > 0 ? args[0] : Undefined.instance;

        if (self.length == 0) return Boolean.FALSE;

        long start;
        if (args.length < 2) {
            start = 0;
        } else {
            start = (long) ScriptRuntime.toInteger(args[1]);
            if (start < 0) {
                start += self.length;
                if (start < 0) start = 0;
            }
            if (start > self.length - 1) return Boolean.FALSE;
        }
        for (int i = (int) start; i < self.length; i++) {
            Object val = self.js_get(i);
            if (ScriptRuntime.sameZero(val, compareTo)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    private static Object js_indexOf(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args, RealThis realThis) {
        NativeTypedArrayView<?> self = realThis.realThis(thisObj);

        Object compareTo = args.length > 0 ? args[0] : Undefined.instance;

        if (self.length == 0) return -1;

        long start;
        if (args.length < 2) {
            // default
            start = 0;
        } else {
            start = (long) ScriptRuntime.toInteger(args[1]);
            if (start < 0) {
                start += self.length;
                if (start < 0) start = 0;
            }
            if (start > self.length - 1) return -1;
        }
        for (int i = (int) start; i < self.length; i++) {
            Object val = self.js_get(i);
            if (val != NOT_FOUND && ScriptRuntime.shallowEq(val, compareTo)) {
                return (long) i;
            }
        }
        return -1;
    }

    private static Object js_lastIndexOf(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args, RealThis realThis) {
        NativeTypedArrayView<?> self = realThis.realThis(thisObj);

        Object compareTo = args.length > 0 ? args[0] : Undefined.instance;

        if (self.length == 0) return -1;

        long start;
        if (args.length < 2) {
            // default
            start = self.length - 1L;
        } else {
            start = (long) ScriptRuntime.toInteger(args[1]);
            if (start >= self.length) start = self.length - 1L;
            else if (start < 0) start += self.length;
            if (start < 0) return -1;
        }
        for (int i = (int) start; i >= 0; i--) {
            Object val = self.js_get(i);
            if (val != NOT_FOUND && ScriptRuntime.shallowEq(val, compareTo)) {
                return (long) i;
            }
        }
        return -1;
    }

    private static Scriptable js_slice(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args, RealThis realThis) {
        NativeTypedArrayView<?> self = realThis.realThis(thisObj);

        long begin, end;
        if (args.length == 0) {
            begin = 0;
            end = self.length;
        } else {
            begin =
                    ArrayLikeAbstractOperations.toSliceIndex(
                            ScriptRuntime.toInteger(args[0]), self.length);
            if (args.length == 1 || args[1] == Undefined.instance) {
                end = self.length;
            } else {
                end =
                        ArrayLikeAbstractOperations.toSliceIndex(
                                ScriptRuntime.toInteger(args[1]), self.length);
            }
        }

        if (end - begin > Integer.MAX_VALUE) {
            String msg = ScriptRuntime.getMessageById("msg.arraylength.bad");
            throw ScriptRuntime.rangeError(msg);
        }

        long count = Math.max(end - begin, 0);

        return self.typedArraySpeciesCreate(
                cx,
                scope,
                new Object[] {
                    self.arrayBuffer, begin * self.getBytesPerElement(), Math.max(0, end - begin)
                },
                "slice");
    }

    private static String js_join(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args, RealThis realThis) {
        NativeTypedArrayView<?> self = realThis.realThis(thisObj);

        // if no args, use "," as separator
        String separator =
                (args.length < 1 || args[0] == Undefined.instance)
                        ? ","
                        : ScriptRuntime.toString(args[0]);
        if (self.length == 0) {
            return "";
        }
        String[] buf = new String[self.length];
        int total_size = 0;
        for (int i = 0; i != self.length; i++) {
            Object temp = self.js_get(i);
            if (temp != null && temp != Undefined.instance) {
                String str = ScriptRuntime.toString(temp);
                total_size += str.length();
                buf[i] = str;
            }
        }
        total_size += (self.length - 1) * separator.length();
        StringBuilder sb = new StringBuilder(total_size);
        for (int i = 0; i != self.length; i++) {
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
        NativeTypedArrayView<?> self = realThis.realThis(thisObj);

        for (int i = 0, j = self.length - 1; i < j; i++, j--) {
            Object temp = self.js_get(i);
            self.js_set(i, self.js_get(j));
            self.js_set(j, temp);
        }
        return self;
    }

    private static NativeTypedArrayView<?> js_fill(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args, RealThis realThis) {
        NativeTypedArrayView<?> self = realThis.realThis(thisObj);

        long relativeStart = 0;
        if (args.length >= 2) {
            relativeStart = (long) ScriptRuntime.toInteger(args[1]);
        }
        final long k;
        if (relativeStart < 0) {
            k = Math.max((self.length + relativeStart), 0);
        } else {
            k = Math.min(relativeStart, self.length);
        }

        long relativeEnd = self.length;
        if (args.length >= 3 && !Undefined.isUndefined(args[2])) {
            relativeEnd = (long) ScriptRuntime.toInteger(args[2]);
        }
        final long fin;
        if (relativeEnd < 0) {
            fin = Math.max((self.length + relativeEnd), 0);
        } else {
            fin = Math.min(relativeEnd, self.length);
        }

        Object value = args.length > 0 ? args[0] : Undefined.instance;
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

        NativeTypedArrayView<?> self = realThis.realThis(thisObj);

        Object[] working = self.sortTemporaryArray(cx, scope, args);
        for (int i = 0; i < self.length; ++i) {
            self.js_set(i, working[i]);
        }

        return self;
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
        NativeTypedArrayView<?> self = realThis.realThis(thisObj);

        Object targetArg = (args.length >= 1) ? args[0] : Undefined.instance;
        long relativeTarget = (long) ScriptRuntime.toInteger(targetArg);
        long to;
        if (relativeTarget < 0) {
            to = Math.max((self.length + relativeTarget), 0);
        } else {
            to = Math.min(relativeTarget, self.length);
        }

        Object startArg = (args.length >= 2) ? args[1] : Undefined.instance;
        long relativeStart = (long) ScriptRuntime.toInteger(startArg);
        long from;
        if (relativeStart < 0) {
            from = Math.max((self.length + relativeStart), 0);
        } else {
            from = Math.min(relativeStart, self.length);
        }

        long relativeEnd = self.length;
        if (args.length >= 3 && !Undefined.isUndefined(args[2])) {
            relativeEnd = (long) ScriptRuntime.toInteger(args[2]);
        }
        final long fin;
        if (relativeEnd < 0) {
            fin = Math.max((self.length + relativeEnd), 0);
        } else {
            fin = Math.min(relativeEnd, self.length);
        }

        long count = Math.min(fin - from, self.length - to);
        int direction = 1;
        if (from < to && to < from + count) {
            direction = -1;
            from = from + count - 1;
            to = to + count - 1;
        }

        for (; count > 0; count--) {
            final Object temp = self.js_get((int) from);
            self.js_set((int) to, temp);
            from += direction;
            to += direction;
        }

        return self;
    }

    private static Object js_set(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args, RealThis realThis) {
        NativeTypedArrayView<?> self = realThis.realThis(thisObj);
        if (args.length > 0) {
            if (args[0] instanceof NativeTypedArrayView) {
                int offset = isArg(args, 1) ? ScriptRuntime.toInt32(args[1]) : 0;
                NativeTypedArrayView<?> nativeView = (NativeTypedArrayView<?>) args[0];
                self.setRange(nativeView, offset);
                return Undefined.instance;
            }
            if (args[0] instanceof NativeArray) {
                int offset = isArg(args, 1) ? ScriptRuntime.toInt32(args[1]) : 0;
                self.setRange((NativeArray) args[0], offset);
                return Undefined.instance;
            }
            if (args[0] instanceof Scriptable) {
                // Tests show that we need to ignore a non-array object
                return Undefined.instance;
            }
            if (isArg(args, 2)) {
                return self.js_set(ScriptRuntime.toInt32(args[0]), args[1]);
            }
        }
        throw ScriptRuntime.constructError("Error", "invalid arguments");
    }

    private static Object js_subarray(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args, RealThis realThis) {
        NativeTypedArrayView<?> self = realThis.realThis(thisObj);

        int start = isArg(args, 0) ? ScriptRuntime.toInt32(args[0]) : 0;
        int end = isArg(args, 1) ? ScriptRuntime.toInt32(args[1]) : self.length;
        if (cx.getLanguageVersion() >= Context.VERSION_ES6 || args.length > 0) {
            start = (start < 0 ? self.length + start : start);
            end = (end < 0 ? self.length + end : end);

            // Clamping behavior as described by the spec.
            start = Math.max(0, start);
            end = Math.min(self.length, end);
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
        NativeTypedArrayView<?> self = realThis.realThis(thisObj);

        long relativeIndex = 0;
        if (args.length >= 1) {
            relativeIndex = (long) ScriptRuntime.toInteger(args[0]);
        }

        long k = (relativeIndex >= 0) ? relativeIndex : self.length + relativeIndex;

        if ((k < 0) || (k >= self.length)) {
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
        return newArray;
    }

    private static Object js_toReversed(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args, RealThis realThis) {
        NativeTypedArrayView<?> self = realThis.realThis(thisObj);

        NativeArrayBuffer newBuffer =
                new NativeArrayBuffer(self.length * self.getBytesPerElement());
        Scriptable result =
                cx.newObject(
                        scope,
                        self.getClassName(),
                        new Object[] {newBuffer, 0, self.length, self.getBytesPerElement()});

        for (int k = 0; k < self.length; ++k) {
            int from = self.length - k - 1;
            Object fromValue = self.js_get(from);
            result.put(k, result, fromValue);
        }

        return result;
    }

    private static Object js_toSorted(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args, RealThis realThis) {
        NativeTypedArrayView<?> self = realThis.realThis(thisObj);

        Object[] working = self.sortTemporaryArray(cx, scope, args);

        // Move value in a new typed array of the same type
        NativeArrayBuffer newBuffer =
                new NativeArrayBuffer(self.length * self.getBytesPerElement());
        Scriptable result =
                cx.newObject(
                        scope,
                        self.getClassName(),
                        new Object[] {newBuffer, 0, self.length, self.getBytesPerElement()});
        for (int k = 0; k < self.length; ++k) {
            result.put(k, result, working[k]);
        }

        return result;
    }

    private static Object js_with(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args, RealThis realThis) {
        NativeTypedArrayView<?> self = realThis.realThis(thisObj);

        long relativeIndex = args.length > 0 ? (int) ScriptRuntime.toInteger(args[0]) : 0;
        long actualIndex = relativeIndex >= 0 ? relativeIndex : self.length + relativeIndex;

        Object argsValue = args.length > 1 ? ScriptRuntime.toNumber(args[1]) : 0.0;

        if (actualIndex < 0 || actualIndex >= self.length) {
            String msg =
                    ScriptRuntime.getMessageById(
                            "msg.typed.array.index.out.of.bounds",
                            relativeIndex,
                            self.length * -1,
                            self.length - 1);
            throw ScriptRuntime.rangeError(msg);
        }

        NativeArrayBuffer newBuffer =
                new NativeArrayBuffer(self.length * self.getBytesPerElement());
        Scriptable result =
                cx.newObject(
                        scope,
                        self.getClassName(),
                        new Object[] {newBuffer, 0, self.length, self.getBytesPerElement()});

        for (int k = 0; k < self.length; ++k) {
            Object fromValue = (k == actualIndex) ? argsValue : self.js_get(k);
            result.put(k, result, fromValue);
        }

        return result;
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
        if (checkIndex(start)) {
            throw new IndexOutOfBoundsException();
        }
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
