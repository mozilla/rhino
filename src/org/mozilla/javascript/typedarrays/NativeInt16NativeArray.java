/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

public class NativeInt16NativeArray
    extends NativeTypedArrayView<Short>
{
    private static final long serialVersionUID = -8592870435287581398L;

    private static final String CLASS_NAME = "Int16Array";
    private static final int BYTES_PER_ELEMENT = 2;

    public NativeInt16NativeArray()
    {
    }

    public NativeInt16NativeArray(NativeArrayBuffer ab, int off, int len)
    {
        super(ab, off, len, len * BYTES_PER_ELEMENT);
    }

    @Override
    public String getClassName()
    {
        return CLASS_NAME;
    }

    public static void init(Scriptable scope, boolean sealed)
    {
        NativeInt16NativeArray a = new NativeInt16NativeArray();
        a.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    @Override
    protected NativeTypedArrayView construct(NativeArrayBuffer ab, int off, int len)
    {
        return new NativeInt16NativeArray(ab, off, len);
    }

    @Override
    protected int getBytesPerElement()
    {
        return BYTES_PER_ELEMENT;
    }

    @Override
    protected NativeTypedArrayView realThis(Scriptable thisObj, IdFunctionObject f)
    {
        if (!(thisObj instanceof NativeInt16NativeArray)) {
            throw incompatibleCallError(f);
        }
        return (NativeInt16NativeArray)thisObj;
    }

    @Override
    protected Object js_get(int index)
    {
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        return ByteIo.readInt16(arrayBuffer.buffer, (index * BYTES_PER_ELEMENT) + offset, false);
    }

    @Override
    protected Object js_set(int index, Object c)
    {
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        int val = Conversions.toInt16(c);
        ByteIo.writeInt16(arrayBuffer.buffer, (index * BYTES_PER_ELEMENT) + offset, val, false);
        return null;
    }

    @Override
    public Short get(int i)
    {
        if (checkIndex(i)) {
            throw new IndexOutOfBoundsException();
        }
        return (Short)js_get(i);
    }

    @Override
    public Short set(int i, Short aByte)
    {
        if (checkIndex(i)) {
            throw new IndexOutOfBoundsException();
        }
        return (Short)js_set(i, aByte);
    }
}
