/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

public class NativeUint8NativeArray
    extends NativeTypedArrayView<Short>
{
    private static final long serialVersionUID = -3349419704390398895L;

    private static final String CLASS_NAME = "Uint8Array";

    public NativeUint8NativeArray()
    {
    }

    public NativeUint8NativeArray(NativeArrayBuffer ab, int off, int len)
    {
        super(ab, off, len, len);
    }

    @Override
    public String getClassName()
    {
        return CLASS_NAME;
    }

    public static void init(Scriptable scope, boolean sealed)
    {
        NativeUint8NativeArray a = new NativeUint8NativeArray();
        a.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    @Override
    protected NativeTypedArrayView construct(NativeArrayBuffer ab, int off, int len)
    {
        return new NativeUint8NativeArray(ab, off, len);
    }

    @Override
    protected int getBytesPerElement()
    {
        return 1;
    }

    @Override
    protected NativeTypedArrayView realThis(Scriptable thisObj, IdFunctionObject f)
    {
        if (!(thisObj instanceof NativeUint8NativeArray)) {
            throw incompatibleCallError(f);
        }
        return (NativeUint8NativeArray)thisObj;
    }

    @Override
    protected Object js_get(int index)
    {
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        return ByteIo.readUint8(arrayBuffer.buffer, index + offset);
    }

    @Override
    protected Object js_set(int index, Object c)
    {
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        int val = Conversions.toUint8(c);
        ByteIo.writeUint8(arrayBuffer.buffer, index + offset, val);
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
