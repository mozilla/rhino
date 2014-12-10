/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

public class Float32NativeArray
    extends NativeTypedArrayView
{
    private static final long serialVersionUID = -8963461831950499340L;

    private static final String CLASS_NAME = "Float32Array";
    private static final int BYTES_PER_ELEMENT = 4;

    public Float32NativeArray()
    {
    }

    public Float32NativeArray(NativeArrayBuffer ab, int off, int len)
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
        Float32NativeArray a = new Float32NativeArray();
        a.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    @Override
    protected NativeTypedArrayView construct(NativeArrayBuffer ab, int off, int len)
    {
        return new Float32NativeArray(ab, off, len);
    }

    @Override
    protected int getBytesPerElement()
    {
        return BYTES_PER_ELEMENT;
    }

    @Override
    protected NativeTypedArrayView realThis(Scriptable thisObj, IdFunctionObject f)
    {
        if (!(thisObj instanceof Float32NativeArray)) {
            throw incompatibleCallError(f);
        }
        return (Float32NativeArray)thisObj;
    }

    @Override
    protected Object js_get(int index)
    {
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        return ByteIo.readFloat32(arrayBuffer.buffer, (index * BYTES_PER_ELEMENT) + offset, false);
    }

    @Override
    protected Object js_set(int index, Object c)
    {
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        double val = ScriptRuntime.toNumber(c);
        ByteIo.writeFloat32(arrayBuffer.buffer, (index * BYTES_PER_ELEMENT) + offset, val, false);
        return null;
    }
}
