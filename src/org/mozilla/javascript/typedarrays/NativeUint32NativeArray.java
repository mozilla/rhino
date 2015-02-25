/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

public class NativeUint32NativeArray
    extends NativeTypedArrayView<Long>
{
    private static final long serialVersionUID = -7987831421954144244L;

    private static final String CLASS_NAME = "Uint32Array";
    private static final int BYTES_PER_ELEMENT = 4;

    public NativeUint32NativeArray()
    {
    }

    public NativeUint32NativeArray(NativeArrayBuffer ab, int off, int len)
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
        NativeUint32NativeArray a = new NativeUint32NativeArray();
        a.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    @Override
    protected NativeTypedArrayView construct(NativeArrayBuffer ab, int off, int len)
    {
        return new NativeUint32NativeArray(ab, off, len);
    }

    @Override
    protected int getBytesPerElement()
    {
        return BYTES_PER_ELEMENT;
    }

    @Override
    protected NativeTypedArrayView realThis(Scriptable thisObj, IdFunctionObject f)
    {
        if (!(thisObj instanceof NativeUint32NativeArray)) {
            throw incompatibleCallError(f);
        }
        return (NativeUint32NativeArray)thisObj;
    }

    @Override
    protected Object js_get(int index)
    {
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        return ByteIo.readUint32(arrayBuffer.buffer, (index * BYTES_PER_ELEMENT) + offset, false);
    }

    @Override
    protected Object js_set(int index, Object c)
    {
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        long val = Conversions.toUint32(c);
        ByteIo.writeUint32(arrayBuffer.buffer, (index * BYTES_PER_ELEMENT) + offset, val, false);
        return null;
    }

    @Override
    public Long get(int i)
    {
        if (checkIndex(i)) {
            throw new IndexOutOfBoundsException();
        }
        return (Long)js_get(i);
    }

    @Override
    public Long set(int i, Long aByte)
    {
        if (checkIndex(i)) {
            throw new IndexOutOfBoundsException();
        }
        return (Long)js_set(i, aByte);
    }
}
