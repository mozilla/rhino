/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.RandomAccess;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ExternalArrayData;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeArrayIterator;
import org.mozilla.javascript.NativeArrayIterator.ARRAY_ITERATOR_TYPE;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Symbol;
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

    protected boolean checkIndex(int index) {
        return ((index < 0) || (index >= length));
    }

    /**
     * Return the number of bytes represented by each element in the array. This can be useful when
     * wishing to manipulate the byte array directly from Java.
     */
    public abstract int getBytesPerElement();

    protected abstract NativeTypedArrayView<T> construct(NativeArrayBuffer ab, int off, int len);

    protected abstract Object js_get(int index);

    protected abstract Object js_set(int index, Object c);

    protected abstract NativeTypedArrayView<T> realThis(Scriptable thisObj, IdFunctionObject f);

    private NativeArrayBuffer makeArrayBuffer(Context cx, Scriptable scope, int length) {
        return (NativeArrayBuffer)
                cx.newObject(
                        scope,
                        NativeArrayBuffer.CLASS_NAME,
                        new Object[] {Double.valueOf((double) length * getBytesPerElement())});
    }

    private NativeTypedArrayView<T> js_constructor(Context cx, Scriptable scope, Object[] args) {
        if (!isArg(args, 0)) {
            return construct(new NativeArrayBuffer(), 0, 0);
        }

        final Object arg0 = args[0];
        if (arg0 == null) {
            return construct(new NativeArrayBuffer(), 0, 0);
        }

        if ((arg0 instanceof Number) || (arg0 instanceof String)) {
            // Create a zeroed-out array of a certain length
            int length = ScriptRuntime.toInt32(arg0);
            NativeArrayBuffer buffer = makeArrayBuffer(cx, scope, length);
            return construct(buffer, 0, length);
        }

        if (arg0 instanceof NativeTypedArrayView) {
            // Copy elements from the old array and convert them into our own
            @SuppressWarnings("unchecked")
            NativeTypedArrayView<T> src = (NativeTypedArrayView<T>) arg0;
            NativeArrayBuffer na = makeArrayBuffer(cx, scope, src.length);
            NativeTypedArrayView<T> v = construct(na, 0, src.length);

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
                byteLen = ScriptRuntime.toInt32(args[2]) * getBytesPerElement();
            } else {
                byteLen = na.getLength() - byteOff;
            }

            if ((byteOff < 0) || (byteOff > na.buffer.length)) {
                throw ScriptRuntime.rangeError("offset out of range");
            }
            if ((byteLen < 0) || ((byteOff + byteLen) > na.buffer.length)) {
                throw ScriptRuntime.rangeError("length out of range");
            }
            if ((byteOff % getBytesPerElement()) != 0) {
                throw ScriptRuntime.rangeError("offset must be a multiple of the byte size");
            }
            if ((byteLen % getBytesPerElement()) != 0) {
                throw ScriptRuntime.rangeError(
                        "offset and buffer must be a multiple of the byte size");
            }

            return construct(na, byteOff, byteLen / getBytesPerElement());
        }

        if (arg0 instanceof NativeArray) {
            // Copy elements of the array and convert them to the correct type
            NativeArray array = (NativeArray) arg0;

            NativeArrayBuffer na = makeArrayBuffer(cx, scope, array.size());
            NativeTypedArrayView<T> v = construct(na, 0, array.size());
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

            NativeArrayBuffer na = makeArrayBuffer(cx, scope, arrayElements.length);
            NativeTypedArrayView<T> v = construct(na, 0, arrayElements.length);
            for (int i = 0; i < arrayElements.length; i++) {
                v.js_set(i, arrayElements[i]);
            }
            return v;
        }
        throw ScriptRuntime.constructError("Error", "invalid argument");
    }

    private void setRange(NativeTypedArrayView<T> v, int off) {
        if (off >= length) {
            throw ScriptRuntime.rangeError("offset out of range");
        }

        if (v.length > (length - off)) {
            throw ScriptRuntime.rangeError("source array too long");
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
        if (off > length) {
            throw ScriptRuntime.rangeError("offset out of range");
        }
        if ((off + a.size()) > length) {
            throw ScriptRuntime.rangeError("offset + length out of range");
        }

        int pos = off;
        for (Object val : a) {
            js_set(pos, val);
            pos++;
        }
    }

    private Object js_subarray(Context cx, Scriptable scope, int s, int e) {
        int start = (s < 0 ? length + s : s);
        int end = (e < 0 ? length + e : e);

        // Clamping behavior as described by the spec.
        start = Math.max(0, start);
        end = Math.min(length, end);
        int len = Math.max(0, (end - start));
        int byteOff =
                Math.min(getByteOffset() + start * getBytesPerElement(), arrayBuffer.getLength());

        return cx.newObject(
                scope,
                getClassName(),
                new Object[] {arrayBuffer, Integer.valueOf(byteOff), Integer.valueOf(len)});
    }

    private Object js_at(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        long relativeIndex = 0;
        if (args.length >= 1) {
            relativeIndex = (long) ScriptRuntime.toInteger(args[0]);
        }

        long k = (relativeIndex >= 0) ? relativeIndex : length + relativeIndex;

        if ((k < 0) || (k >= length)) {
            return Undefined.instance;
        }

        return getProperty(thisObj, (int) k);
    }

    // Dispatcher

    @Override
    public Object execIdCall(
            IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(getClassName())) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        switch (id) {
            case Id_constructor:
                if (thisObj != null && cx.getLanguageVersion() >= Context.VERSION_ES6) {
                    throw ScriptRuntime.typeErrorById("msg.only.from.new", getClassName());
                }
                return js_constructor(cx, scope, args);

            case Id_toString:
                NativeTypedArrayView<T> realThis = realThis(thisObj, f);
                final int arrayLength = realThis.getArrayLength();
                final StringBuilder builder = new StringBuilder();
                if (arrayLength > 0) {
                    builder.append(ScriptRuntime.toString(realThis.js_get(0)));
                }
                for (int i = 1; i < arrayLength; i++) {
                    builder.append(',');
                    builder.append(ScriptRuntime.toString(realThis.js_get(i)));
                }
                return builder.toString();

            case Id_get:
                if (args.length > 0) {
                    return realThis(thisObj, f).js_get(ScriptRuntime.toInt32(args[0]));
                }
                throw ScriptRuntime.constructError("Error", "invalid arguments");

            case Id_set:
                if (args.length > 0) {
                    NativeTypedArrayView<T> self = realThis(thisObj, f);
                    if (args[0] instanceof NativeTypedArrayView) {
                        int offset = isArg(args, 1) ? ScriptRuntime.toInt32(args[1]) : 0;
                        @SuppressWarnings("unchecked")
                        NativeTypedArrayView<T> nativeView = (NativeTypedArrayView<T>) args[0];
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

            case Id_subarray:
                NativeTypedArrayView<T> self = realThis(thisObj, f);
                int start = isArg(args, 0) ? ScriptRuntime.toInt32(args[0]) : 0;
                int end = isArg(args, 1) ? ScriptRuntime.toInt32(args[1]) : self.length;
                if (cx.getLanguageVersion() >= Context.VERSION_ES6 || args.length > 0) {
                    return self.js_subarray(cx, scope, start, end);
                }
                throw ScriptRuntime.constructError("Error", "invalid arguments");

            case Id_at:
                NativeTypedArrayView<T> atSelf = realThis(thisObj, f);
                if (cx.getLanguageVersion() >= Context.VERSION_ES6 || args.length > 0) {
                    return atSelf.js_at(cx, scope, thisObj, args);
                }
                throw ScriptRuntime.constructError("Error", "invalid arguments");

            case SymbolId_iterator:
                return new NativeArrayIterator(scope, thisObj, ARRAY_ITERATOR_TYPE.VALUES);
        }
        throw new IllegalArgumentException(String.valueOf(id));
    }

    @Override
    protected void initPrototypeId(int id) {
        if (id == SymbolId_iterator) {
            initPrototypeMethod(getClassName(), id, SymbolKey.ITERATOR, "[Symbol.iterator]", 0);
            return;
        }

        String s, fnName = null;
        int arity;
        switch (id) {
            case Id_constructor:
                arity = 3;
                s = "constructor";
                break;
            case Id_toString:
                arity = 0;
                s = "toString";
                break;
            case Id_get:
                arity = 1;
                s = "get";
                break;
            case Id_set:
                arity = 2;
                s = "set";
                break;
            case Id_subarray:
                arity = 2;
                s = "subarray";
                break;
            case Id_at:
                arity = 1;
                s = "at";
                break;
            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(getClassName(), id, s, fnName, arity);
    }

    @Override
    protected int findPrototypeId(Symbol k) {
        if (SymbolKey.ITERATOR.equals(k)) {
            return SymbolId_iterator;
        }
        return 0;
    }

    @Override
    protected int findPrototypeId(String s) {
        int id;
        switch (s) {
            case "constructor":
                id = Id_constructor;
                break;
            case "toString":
                id = Id_toString;
                break;
            case "get":
                id = Id_get;
                break;
            case "set":
                id = Id_set;
                break;
            case "subarray":
                id = Id_subarray;
                break;
            case "at":
                id = Id_at;
                break;
            default:
                id = 0;
                break;
        }
        return id;
    }

    // Table of all functions
    private static final int Id_constructor = 1,
            Id_toString = 2,
            Id_get = 3,
            Id_set = 4,
            Id_subarray = 5,
            Id_at = 6,
            SymbolId_iterator = 7;

    protected static final int MAX_PROTOTYPE_ID = SymbolId_iterator;

    // Constructor properties

    @Override
    protected void fillConstructorProperties(IdFunctionObject ctor) {
        ctor.defineProperty(
                "BYTES_PER_ELEMENT",
                ScriptRuntime.wrapInt(getBytesPerElement()),
                DONTENUM | PERMANENT | READONLY);

        super.fillConstructorProperties(ctor);
    }

    // Property dispatcher

    @Override
    protected int getMaxInstanceId() {
        return MAX_INSTANCE_ID;
    }

    @Override
    protected String getInstanceIdName(int id) {
        switch (id) {
            case Id_length:
                return "length";
            case Id_BYTES_PER_ELEMENT:
                return "BYTES_PER_ELEMENT";
            default:
                return super.getInstanceIdName(id);
        }
    }

    @Override
    protected Object getInstanceIdValue(int id) {
        switch (id) {
            case Id_length:
                return ScriptRuntime.wrapInt(length);
            case Id_BYTES_PER_ELEMENT:
                return ScriptRuntime.wrapInt(getBytesPerElement());
            default:
                return super.getInstanceIdValue(id);
        }
    }

    @Override
    protected int findInstanceIdInfo(String s) {
        int id;
        switch (s) {
            case "length":
                id = Id_length;
                break;
            case "BYTES_PER_ELEMENT":
                id = Id_BYTES_PER_ELEMENT;
                break;
            default:
                id = 0;
                break;
        }
        if (id == 0) {
            return super.findInstanceIdInfo(s);
        }
        if (id == Id_BYTES_PER_ELEMENT) {
            return instanceIdInfo(DONTENUM | READONLY | PERMANENT, id);
        }
        return instanceIdInfo(READONLY | PERMANENT, id);
    }

    /*
     * These must not conflict with ids in the parent since we delegate there for property dispatching.
     */
    private static final int Id_length = NativeArrayBufferView.MAX_INSTANCE_ID + 1,
            Id_BYTES_PER_ELEMENT = Id_length + 1,
            MAX_INSTANCE_ID = Id_BYTES_PER_ELEMENT;

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
        try {
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
        } catch (ClassCastException cce) {
            return false;
        }
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
