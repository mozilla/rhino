/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

/**
 * A NativeArrayBuffer is the backing buffer for a typed array. Used inside JavaScript code,
 * it implements the ArrayBuffer interface. Used directly from Java, it simply holds a byte array.
 */

public class NativeArrayBuffer
    extends IdScriptableObject
{
    private static final long serialVersionUID = 3110411773054879549L;

    public static final String CLASS_NAME = "ArrayBuffer";

    private static final byte[] EMPTY_BUF = new byte[0];

    final byte[] buffer;

    @Override
    public String getClassName()
    {
        return CLASS_NAME;
    }

    public static void init(Context cx, Scriptable scope, boolean sealed)
    {
        NativeArrayBuffer na = new NativeArrayBuffer();
        na.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    /**
     * Create an empty buffer.
     */
    public NativeArrayBuffer()
    {
        buffer = EMPTY_BUF;
    }

    /**
     * Create a buffer of the specified length in bytes.
     */
    public NativeArrayBuffer(double len)
    {
        if (len >= Integer.MAX_VALUE) {
            throw ScriptRuntime.constructError("RangeError", "length parameter (" + len + ") is too large ");
        }
        if (len == Double.NEGATIVE_INFINITY) {
            throw ScriptRuntime.constructError("RangeError", "Negative array length " + len);
        }

        int intLen = ScriptRuntime.toInt32(len);
        if (intLen < 0) {
            throw ScriptRuntime.constructError("RangeError", "Negative array length " + len);
        }
        if (intLen == 0) {
            buffer = EMPTY_BUF;
        } else {
            buffer = new byte[intLen];
        }
    }

    /**
     * Get the number of bytes in the buffer.
     */
    public int getLength() {
        return buffer.length;
    }

    /**
     * Return the actual bytes that back the buffer. This is a reference to the real buffer,
     * so changes to bytes here will be reflected in the actual object and all its views.
     */
    public byte[] getBuffer() {
        return buffer;
    }

    // Actual implementations of actual code

    /**
     * Return a new buffer that represents a slice of this buffer's content, starting at position
     * "start" and ending at position "end". Both values will be "clamped" as per the JavaScript
     * spec so that invalid values may be passed and will be adjusted up or down accordingly.
     * This method will return a new buffer that contains a copy of the original buffer. Changes
     * there will not affect the content of the buffer.
     *
     * @param s the position where the new buffer will start
     * @param e the position where it will end
     */
    public NativeArrayBuffer slice(double s, double e)
    {
        // Handle negative start as relative to start
        // Clamp as per the spec to between 0 and length
        int end = ScriptRuntime.toInt32(Math.max(0, Math.min(buffer.length, (e < 0 ? buffer.length + e : e))));
        int start = ScriptRuntime.toInt32(Math.min(end, Math.max(0, (s < 0 ? buffer.length + s : s))));
        int len = end - start;

        NativeArrayBuffer newBuf = new NativeArrayBuffer(len);
        System.arraycopy(buffer, start, newBuf.buffer, 0, len);
        return newBuf;
    }

    // Function-calling dispatcher

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Scriptable thisObj, Object[] args)
    {
        if (!f.hasTag(CLASS_NAME)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        switch (id) {
        case ConstructorId_isView:
            return (isArg(args, 0) && (args[0] instanceof NativeArrayBufferView));

        case Id_constructor:
            double length = isArg(args, 0) ? ScriptRuntime.toNumber(args[0]) : 0;
            return new NativeArrayBuffer(length);

        case Id_slice:
            NativeArrayBuffer self = realThis(thisObj, f);
            double start = isArg(args, 0) ? ScriptRuntime.toNumber(args[0]) : 0;
            double end = isArg(args, 1) ? ScriptRuntime.toNumber(args[1]) : self.buffer.length;
            return self.slice(start, end);
        }
        throw new IllegalArgumentException(String.valueOf(id));
    }

    private static NativeArrayBuffer realThis(Scriptable thisObj, IdFunctionObject f)
    {
        if (!(thisObj instanceof NativeArrayBuffer))
            throw incompatibleCallError(f);
        return (NativeArrayBuffer)thisObj;
    }

    private static boolean isArg(Object[] args, int i)
    {
        return ((args.length > i) && !Undefined.instance.equals(args[i]));
    }

    @Override
    protected void initPrototypeId(int id)
    {
        String s;
        int arity;
        switch (id) {
        case Id_constructor:            arity = 1; s = "constructor"; break;
        case Id_slice:                  arity = 1; s = "slice"; break;
        default: throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(CLASS_NAME, id, s, arity);
    }

// #string_id_map#

    @Override
    protected int findPrototypeId(String s)
    {
        int id;
// #generated# Last update: 2018-07-20 08:21:54 MESZ
        L0: { id = 0; String X = null;
            int s_length = s.length();
            if (s_length==5) { X="slice";id=Id_slice; }
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
        Id_slice                = 2,
        MAX_PROTOTYPE_ID        = Id_slice;

// #/string_id_map#

    // Constructor (aka static) functions here

    private static final int ConstructorId_isView = -1;

    @Override
    protected void fillConstructorProperties(IdFunctionObject ctor)
    {
        addIdFunctionProperty(ctor, CLASS_NAME, ConstructorId_isView, "isView", 1);
    }

    // Properties here

    @Override
    protected int getMaxInstanceId()
    {
        return MAX_INSTANCE_ID;
    }

    @Override
    protected String getInstanceIdName(int id)
    {
        if (id == Id_byteLength) { return "byteLength"; }
        return super.getInstanceIdName(id);
    }

    @Override
    protected Object getInstanceIdValue(int id)
    {
        if (id == Id_byteLength) {
            return ScriptRuntime.wrapInt(buffer.length);
        }
        return super.getInstanceIdValue(id);
    }

    @Override
    protected int findInstanceIdInfo(String s)
    {
        if ("byteLength".equals(s)) {
            return instanceIdInfo(READONLY | PERMANENT, Id_byteLength);
        }
        return super.findInstanceIdInfo(s);
    }

    // Table of all properties
    private static final int
        Id_byteLength           = 1,
        MAX_INSTANCE_ID         = Id_byteLength;
}
