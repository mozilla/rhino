/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ExternalArrayData;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeArrayIterator;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Symbol;
import org.mozilla.javascript.SymbolKey;
import org.mozilla.javascript.Undefined;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

/**
 * This class is the abstract parent for all of the various typed arrays. Each one
 * shows a view of a specific NativeArrayBuffer, and modifications here will affect the rest.
 */

public abstract class NativeTypedArrayView<T>
    extends NativeArrayBufferView
    implements List<T>, RandomAccess, ExternalArrayData
{
    /** The length, in elements, of the array */
    protected final int length;

    protected NativeTypedArrayView()
    {
        super();
        length = 0;
    }

    protected NativeTypedArrayView(NativeArrayBuffer ab, int off, int len, int byteLen)
    {
        super(ab, off, byteLen);
        length = len;
    }

    // Array properties implementation

    @Override
    public Object get(int index, Scriptable start)
    {
        return js_get(index);
    }

    @Override
    public boolean has(int index, Scriptable start)
    {
        return ((index > 0) && (index < length));
    }

    @Override
    public void put(int index, Scriptable start, Object val)
    {
        js_set(index, val);
    }

    @Override
    public void delete(int index)
    {
    }

    @Override
    public Object[] getIds()
    {
        Object[] ret = new Object[length];
        for (int i = 0; i < length; i++) {
            ret[i] = Integer.valueOf(i);
        }
        return ret;
    }

    // Actual functions

    protected boolean checkIndex(int index)
    {
       return ((index < 0) || (index >= length));
    }

    /**
     * Return the number of bytes represented by each element in the array. This can be useful
     * when wishing to manipulate the byte array directly from Java.
     */
    public abstract int getBytesPerElement();

    protected abstract NativeTypedArrayView construct(NativeArrayBuffer ab, int off, int len);
    protected abstract Object js_get(int index);
    protected abstract Object js_set(int index, Object c);
    protected abstract NativeTypedArrayView realThis(Scriptable thisObj, IdFunctionObject f);

    private NativeArrayBuffer makeArrayBuffer(Context cx, Scriptable scope, int length)
    {
        return (NativeArrayBuffer)cx.newObject(scope, NativeArrayBuffer.CLASS_NAME,
                                               new Object[] { length });
    }

    private NativeTypedArrayView js_constructor(Context cx, Scriptable scope, Object[] args)
    {
        if (!isArg(args, 0)) {
            return construct(NativeArrayBuffer.EMPTY_BUFFER, 0, 0);

        } else if ((args[0] instanceof Number) || (args[0] instanceof String)) {
            // Create a zeroed-out array of a certain length
            int length = ScriptRuntime.toInt32(args[0]);
            NativeArrayBuffer buffer = makeArrayBuffer(cx, scope, length * getBytesPerElement());
            return construct(buffer, 0, length);

        } else if (args[0] instanceof NativeTypedArrayView) {
            // Copy elements from the old array and convert them into our own
            NativeTypedArrayView src = (NativeTypedArrayView)args[0];
            NativeArrayBuffer na = makeArrayBuffer(cx, scope, src.length * getBytesPerElement());
            NativeTypedArrayView v = construct(na, 0, src.length);

            for (int i = 0; i < src.length; i++) {
                v.js_set(i, src.js_get(i));
            }
            return v;

        } else if (args[0] instanceof NativeArrayBuffer) {
            // Make a slice of an existing buffer, with shared storage
            NativeArrayBuffer na = (NativeArrayBuffer)args[0];
            int byteOff = isArg(args, 1) ? ScriptRuntime.toInt32(args[1]) : 0;

            int byteLen;
            if (isArg(args, 2)) {
                byteLen = ScriptRuntime.toInt32(args[2]) * getBytesPerElement();
            } else {
                byteLen = na.getLength() - byteOff;
            }

            if ((byteOff < 0) || (byteOff > na.buffer.length)) {
                throw ScriptRuntime.constructError("RangeError", "offset out of range");
            }
            if ((byteLen < 0) || ((byteOff + byteLen) > na.buffer.length)) {
                throw ScriptRuntime.constructError("RangeError", "length out of range");
            }
            if ((byteOff % getBytesPerElement()) != 0) {
                throw ScriptRuntime.constructError("RangeError", "offset must be a multiple of the byte size");
            }
            if ((byteLen % getBytesPerElement()) != 0) {
                throw ScriptRuntime.constructError("RangeError", "offset and buffer must be a multiple of the byte size");
            }

            return construct(na, byteOff, byteLen / getBytesPerElement());

        } else if (args[0] instanceof NativeArray) {
            // Copy elements of the array and convert them to the correct type
            List l = (List)args[0];
            NativeArrayBuffer na = makeArrayBuffer(cx, scope, l.size() * getBytesPerElement());
            NativeTypedArrayView v = construct(na, 0, l.size());
            int p = 0;
            for (Object o : l) {
                v.js_set(p, o);
                p++;
            }
            return v;

        } else {
            throw ScriptRuntime.constructError("Error", "invalid argument");
        }
    }

    private void setRange(NativeTypedArrayView v, int off)
    {
        if (off >= length) {
            throw ScriptRuntime.constructError("RangeError", "offset out of range");
        }

        if (v.length > (length - off)) {
            throw ScriptRuntime.constructError("RangeError", "source array too long");
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

    private void setRange(NativeArray a, int off)
    {
        if (off > length) {
            throw ScriptRuntime.constructError("RangeError", "offset out of range");
        }
        if ((off + a.size()) > length) {
            throw ScriptRuntime.constructError("RangeError", "offset + length out of range");
        }

        int pos = off;
        for (Object val : a) {
            js_set(pos, val);
            pos++;
        }
    }

    private Object js_subarray(Context cx, Scriptable scope, int s, int e)
    {
        int start = (s < 0 ? length + s : s);
        int end = (e < 0 ? length + e : e);

        // Clamping behavior as described by the spec.
        start = Math.max(0, start);
        end = Math.min(length, end);
        int len = Math.max(0, (end - start));
        int byteOff = Math.min(start * getBytesPerElement(), arrayBuffer.getLength());

        return
            cx.newObject(scope, getClassName(),
                         new Object[]{arrayBuffer, byteOff, len});
    }

    // Dispatcher

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Scriptable thisObj, Object[] args)
    {
        if (!f.hasTag(getClassName())) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        switch (id) {
        case Id_constructor:
            return js_constructor(cx, scope, args);

        case Id_get:
            if (args.length > 0) {
                return realThis(thisObj, f).js_get(ScriptRuntime.toInt32(args[0]));
            } else {
                throw ScriptRuntime.constructError("Error", "invalid arguments");
            }

        case Id_set:
            if (args.length > 0) {
                NativeTypedArrayView self = realThis(thisObj, f);
                if (args[0] instanceof NativeTypedArrayView) {
                    int offset = isArg(args, 1) ? ScriptRuntime.toInt32(args[1]) : 0;
                    self.setRange((NativeTypedArrayView)args[0], offset);
                    return Undefined.instance;
                }
                if (args[0] instanceof NativeArray) {
                    int offset = isArg(args, 1) ? ScriptRuntime.toInt32(args[1]) : 0;
                    self.setRange((NativeArray)args[0], offset);
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
            if (args.length > 0) {
                NativeTypedArrayView self = realThis(thisObj, f);
                int start = ScriptRuntime.toInt32(args[0]);
                int end = isArg(args, 1) ? ScriptRuntime.toInt32(args[1]) : self.length;
                return self.js_subarray(cx, scope, start, end);
            } else {
                throw ScriptRuntime.constructError("Error", "invalid arguments");
            }

        case SymbolId_iterator:
            return new NativeArrayIterator(scope, thisObj);
        }
        throw new IllegalArgumentException(String.valueOf(id));
    }

    @Override
    protected void initPrototypeId(int id)
    {
        if (id == SymbolId_iterator) {
            initPrototypeMethod(getClassName(), id, SymbolKey.ITERATOR, "[Symbol.iterator]", 0);
            return;
        }

        String s, fnName = null;
        int arity;
        switch (id) {
        case Id_constructor:        arity = 1; s = "constructor"; break;
        case Id_get:                arity = 1; s = "get"; break;
        case Id_set:                arity = 2; s = "set"; break;
        case Id_subarray:           arity = 2; s = "subarray"; break;
        default: throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(getClassName(), id, s, fnName, arity);
    }

    @Override
    protected int findPrototypeId(Symbol k)
    {
        if (SymbolKey.ITERATOR.equals(k)) {
            return SymbolId_iterator;
        }
        return 0;
    }

    // #string_id_map#

    @Override
    protected int findPrototypeId(String s)
    {
        int id;
// #generated# Last update: 2016-03-04 20:59:23 GMT
        L0: { id = 0; String X = null; int c;
            int s_length = s.length();
            if (s_length==3) {
                c=s.charAt(0);
                if (c=='g') { if (s.charAt(2)=='t' && s.charAt(1)=='e') {id=Id_get; break L0;} }
                else if (c=='s') { if (s.charAt(2)=='t' && s.charAt(1)=='e') {id=Id_set; break L0;} }
            }
            else if (s_length==8) { X="subarray";id=Id_subarray; }
            else if (s_length==11) { X="constructor";id=Id_constructor; }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
        return id;
    }

    // Table of all functions
    private static final int
        Id_constructor          = 1,
        Id_get                  = 2,
        Id_set                  = 3,
        Id_subarray             = 4,
        SymbolId_iterator       = 5;

    protected static final int
        MAX_PROTOTYPE_ID        = SymbolId_iterator;

// #/string_id_map#

    // Constructor properties

    @Override
    protected void fillConstructorProperties(IdFunctionObject ctor)
    {
        ctor.put("BYTES_PER_ELEMENT", ctor, ScriptRuntime.wrapInt(getBytesPerElement()));
    }

    // Property dispatcher

    @Override
    protected int getMaxInstanceId()
    {
        return MAX_INSTANCE_ID;
    }

    @Override
    protected String getInstanceIdName(int id)
    {
        switch (id) {
        case Id_length: return "length";
        case Id_BYTES_PER_ELEMENT: return "BYTES_PER_ELEMENT";
        default: return super.getInstanceIdName(id);
        }
    }

    @Override
    protected Object getInstanceIdValue(int id)
    {
        switch (id) {
        case Id_length:
            return ScriptRuntime.wrapInt(length);
        case Id_BYTES_PER_ELEMENT:
            return ScriptRuntime.wrapInt(getBytesPerElement());
        default:
            return super.getInstanceIdValue(id);
        }
    }

// #string_id_map#

    @Override
    protected int findInstanceIdInfo(String s)
    {
        int id;
// #generated# Last update: 2014-12-08 17:33:28 PST
        L0: { id = 0; String X = null;
            int s_length = s.length();
            if (s_length==6) { X="length";id=Id_length; }
            else if (s_length==17) { X="BYTES_PER_ELEMENT";id=Id_BYTES_PER_ELEMENT; }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
        if (id == 0) {
            return super.findInstanceIdInfo(s);
        }
        return instanceIdInfo(READONLY | PERMANENT, id);
    }

    /*
     * These must not conflict with ids in the parent since we delegate there for property dispatching.
     */
    private static final int
        Id_length               = 10,
        Id_BYTES_PER_ELEMENT    = 11,
        MAX_INSTANCE_ID         = Id_BYTES_PER_ELEMENT;

// #/string_id_map#

    // External Array implementation

    @Override
    public Object getArrayElement(int index)
    {
        return js_get(index);
    }

    @Override
    public void setArrayElement(int index, Object value)
    {
        js_set(index, value);
    }

    @Override
    public int getArrayLength() {
        return length;
    }

    // Abstract List implementation

    @SuppressWarnings("unused")
    public int size()
    {
        return length;
    }

    @SuppressWarnings("unused")
    public boolean isEmpty()
    {
        return (length == 0);
    }

    @SuppressWarnings("unused")
    public boolean contains(Object o)
    {
        return (indexOf(o) >= 0);
    }

    @SuppressWarnings("unused")
    public boolean containsAll(Collection<?> objects)
    {
        for (Object o : objects) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unused")
    public int indexOf(Object o)
    {
        for (int i = 0; i < length; i++) {
            if (o.equals(js_get(i))) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings("unused")
    public int lastIndexOf(Object o)
    {
        for (int i = length - 1; i >= 0; i--) {
            if (o.equals(js_get(i))) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings("unused")
    public Object[] toArray()
    {
        Object[] a = new Object[length];
        for (int i = 0; i < length; i++) {
            a[i] = js_get(i);
        }
        return a;
    }

    @SuppressWarnings("unused")
    public <U> U[] toArray(U[] ts)
    {
        U[] a;

        if (ts.length >= length) {
            a = ts;
        } else {
            a = (U[])Array.newInstance(ts.getClass().getComponentType(), length);
        }

        for (int i = 0; i < length; i++) {
            try {
                a[i] = (U)js_get(i);
            } catch (ClassCastException cce) {
                throw new ArrayStoreException();
            }
        }
        return a;
    }

    @Override
    public boolean equals(Object o)
    {
        try {
            NativeTypedArrayView<T> v = (NativeTypedArrayView<T>)o;
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
    public int hashCode()
    {
        int hc = 0;
        for (int i = 0; i < length; i++) {
            hc += js_get(i).hashCode();
        }
        return hc;
    }

    @SuppressWarnings("unused")
    public Iterator<T> iterator()
    {
        return new NativeTypedArrayIterator<T>(this, 0);
    }

    @SuppressWarnings("unused")
    public ListIterator<T> listIterator()
    {
        return new NativeTypedArrayIterator<T>(this, 0);
    }

    @SuppressWarnings("unused")
    public ListIterator<T> listIterator(int start)
    {
        if (checkIndex(start)) {
            throw new IndexOutOfBoundsException();
        }
        return new NativeTypedArrayIterator<T>(this, start);
    }

    @SuppressWarnings("unused")
    public List<T> subList(int i, int i2)
    {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    public boolean add(T aByte)
    {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    public void add(int i, T aByte)
    {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    public boolean addAll(Collection<? extends T> bytes)
    {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    public boolean addAll(int i, Collection<? extends T> bytes)
    {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    public void clear()
    {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    public T remove(int i)
    {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    public boolean remove(Object o)
    {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    public boolean removeAll(Collection<?> objects)
    {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    public boolean retainAll(Collection<?> objects)
    {
        throw new UnsupportedOperationException();
    }
}
