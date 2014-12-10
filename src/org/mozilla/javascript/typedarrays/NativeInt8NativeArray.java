/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

public class NativeInt8NativeArray
    extends NativeTypedArrayView
{
    private static final long serialVersionUID = -3349419704390398895L;

    private static final String CLASS_NAME = "Int8Array";

    public NativeInt8NativeArray()
    {
    }

    public NativeInt8NativeArray(NativeArrayBuffer ab, int off, int len)
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
        NativeInt8NativeArray a = new NativeInt8NativeArray();
        a.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    @Override
    protected NativeTypedArrayView construct(NativeArrayBuffer ab, int off, int len)
    {
        return new NativeInt8NativeArray(ab, off, len);
    }

    @Override
    protected int getBytesPerElement()
    {
        return 1;
    }

    @Override
    protected NativeTypedArrayView realThis(Scriptable thisObj, IdFunctionObject f)
    {
        if (!(thisObj instanceof NativeInt8NativeArray)) {
            throw incompatibleCallError(f);
        }
        return (NativeInt8NativeArray)thisObj;
    }

    @Override
    protected Object js_get(int index)
    {
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        return ByteIo.readInt8(arrayBuffer.buffer, index + offset);
    }

    @Override
    protected Object js_set(int index, Object c)
    {
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        int val = Conversions.toInt8(c);
        ByteIo.writeInt8(arrayBuffer.buffer, index + offset, val);
        return null;
    }
}
