/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Undefined;

/**
 * This class is the abstract parent for all views of the array. It shows a view of the underlying
 * NativeArrayBuffer. Many views may simultaneously share the same buffer, and changes to one will affect all.
 */

public abstract class NativeArrayBufferView
    extends IdScriptableObject
{
    private static final long serialVersionUID = 6884475582973958419L;

    private static Boolean useLittleEndian = null;

    /** Many view objects can share the same backing array */
    protected final NativeArrayBuffer arrayBuffer;
    /** The offset, in bytes, from the start of the backing array */
    protected final int offset;
    /** The length, in bytes, of the portion of the backing array that we use */
    protected final int byteLength;

    public NativeArrayBufferView()
    {
        arrayBuffer = new NativeArrayBuffer();
        offset = 0;
        byteLength = 0;
    }

    protected NativeArrayBufferView(NativeArrayBuffer ab, int offset, int byteLength)
    {
        this.offset = offset;
        this.byteLength = byteLength;
        this.arrayBuffer = ab;
    }

    /**
     * Return the buffer that backs this view.
     */
    public NativeArrayBuffer getBuffer() {
        return arrayBuffer;
    }

    /**
     * Return the offset in bytes from the start of the buffer that this view represents.
     */
    public int getByteOffset() {
        return offset;
    }

    /**
     * Return the length, in bytes, of the part of the buffer that this view represents.
     */
    public int getByteLength() {
        return byteLength;
    }

    protected static boolean useLittleEndian() {
        if (useLittleEndian == null) {
            Context ctx = Context.getCurrentContext();
            // for some unit tests this might be null
            if (ctx == null) {
                return false;
            }
            useLittleEndian = Boolean.valueOf(ctx.hasFeature(Context.FEATURE_LITTLE_ENDIAN));
        }
        return useLittleEndian.booleanValue();
    }

    protected static boolean isArg(Object[] args, int i)
    {
        return ((args.length > i) && !Undefined.instance.equals(args[i]));
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
        case Id_buffer: return "buffer";
        case Id_byteOffset: return "byteOffset";
        case Id_byteLength: return "byteLength";
        default: return super.getInstanceIdName(id);
        }
    }

    @Override
    protected Object getInstanceIdValue(int id)
    {
        switch (id) {
        case Id_buffer:
            return arrayBuffer;
        case Id_byteOffset:
            return ScriptRuntime.wrapInt(offset);
        case Id_byteLength:
            return ScriptRuntime.wrapInt(byteLength);
        default:
            return super.getInstanceIdValue(id);
        }
    }

// #string_id_map#

    @Override
    protected int findInstanceIdInfo(String s)
    {
        int id;
// #generated# Last update: 2021-03-21 09:47:23 MEZ
        switch (s) {
        case "buffer":
            id = Id_buffer;
            break;
        case "byteOffset":
            id = Id_byteOffset;
            break;
        case "byteLength":
            id = Id_byteLength;
            break;
        default:
            id = 0;
            break;
        }
// #/generated#
        if (id == 0) {
            return super.findInstanceIdInfo(s);
        }
        return instanceIdInfo(READONLY | PERMANENT, id);
    }

    private static final int
        Id_buffer               = 1,
        Id_byteOffset           = 2,
        Id_byteLength           = 3;

    // to be visible by subclasses
    protected static final int
        MAX_INSTANCE_ID         = Id_byteLength;

// #/string_id_map#
}
