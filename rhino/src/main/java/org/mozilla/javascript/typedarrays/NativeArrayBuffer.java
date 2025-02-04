/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import org.mozilla.javascript.AbstractEcmaObjectOperations;
import org.mozilla.javascript.Constructable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

/**
 * A NativeArrayBuffer is the backing buffer for a typed array. Used inside JavaScript code, it
 * implements the ArrayBuffer interface. Used directly from Java, it simply holds a byte array.
 */
public class NativeArrayBuffer extends ScriptableObject {
    private static final long serialVersionUID = 3110411773054879549L;

    public static final String CLASS_NAME = "ArrayBuffer";

    private static final byte[] EMPTY_BUF = new byte[0];

    final byte[] buffer;

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    public static Object init(Context cx, Scriptable scope, boolean sealed) {
        LambdaConstructor constructor =
                new LambdaConstructor(
                        scope,
                        "ArrayBuffer",
                        1,
                        LambdaConstructor.CONSTRUCTOR_NEW,
                        NativeArrayBuffer::js_constructor);
        constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);

        constructor.defineConstructorMethod(
                scope, "isView", 1, NativeArrayBuffer::js_isView, DONTENUM, DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "slice",
                2,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        js_slice(lcx, lscope, thisObj, constructor, args),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeProperty(
                cx, "byteLength", NativeArrayBuffer::js_byteLength, DONTENUM | READONLY);

        if (sealed) {
            constructor.sealObject();
        }
        return constructor;
    }

    /** Create an empty buffer. */
    public NativeArrayBuffer() {
        buffer = EMPTY_BUF;
    }

    /** Create a buffer of the specified length in bytes. */
    public NativeArrayBuffer(double len) {
        if (len >= Integer.MAX_VALUE) {
            throw ScriptRuntime.rangeError("length parameter (" + len + ") is too large ");
        }
        if (len == Double.NEGATIVE_INFINITY) {
            throw ScriptRuntime.rangeError("Negative array length " + len);
        }

        // support rounding
        if (len <= -1) {
            throw ScriptRuntime.rangeError("Negative array length " + len);
        }

        int intLen = ScriptRuntime.toInt32(len);
        if (intLen < 0) {
            throw ScriptRuntime.rangeError("Negative array length " + len);
        }
        if (intLen == 0) {
            buffer = EMPTY_BUF;
        } else {
            buffer = new byte[intLen];
        }
    }

    /** Get the number of bytes in the buffer. */
    public int getLength() {
        return buffer.length;
    }

    /**
     * Return the actual bytes that back the buffer. This is a reference to the real buffer, so
     * changes to bytes here will be reflected in the actual object and all its views.
     */
    public byte[] getBuffer() {
        return buffer;
    }

    /**
     * Return a new buffer that represents a slice of this buffer's content, starting at position
     * "start" and ending at position "end". Both values will be "clamped" as per the JavaScript
     * spec so that invalid values may be passed and will be adjusted up or down accordingly. This
     * method will return a new buffer that contains a copy of the original buffer. Changes there
     * will not affect the content of the buffer.
     *
     * @param s the position where the new buffer will start
     * @param e the position where it will end
     */
    public NativeArrayBuffer slice(double s, double e) {
        // Handle negative start as relative to start
        // Clamp as per the spec to between 0 and length
        int end =
                ScriptRuntime.toInt32(
                        Math.max(0, Math.min(buffer.length, (e < 0 ? buffer.length + e : e))));
        int start =
                ScriptRuntime.toInt32(Math.min(end, Math.max(0, (s < 0 ? buffer.length + s : s))));
        int len = end - start;

        NativeArrayBuffer newBuf = new NativeArrayBuffer(len);
        System.arraycopy(buffer, start, newBuf.buffer, 0, len);
        return newBuf;
    }

    private static NativeArrayBuffer getSelf(Scriptable thisObj) {
        return LambdaConstructor.convertThisObject(thisObj, NativeArrayBuffer.class);
    }

    private static NativeArrayBuffer js_constructor(Context cx, Scriptable scope, Object[] args) {
        double length = isArg(args, 0) ? ScriptRuntime.toNumber(args[0]) : 0;
        return new NativeArrayBuffer(length);
    }

    private static Boolean js_isView(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return Boolean.valueOf((isArg(args, 0) && (args[0] instanceof NativeArrayBufferView)));
    }

    private static NativeArrayBuffer js_slice(
            Context cx,
            Scriptable scope,
            Scriptable thisObj,
            LambdaConstructor defaultConstructor,
            Object[] args) {
        NativeArrayBuffer self = getSelf(thisObj);
        double start = isArg(args, 0) ? ScriptRuntime.toNumber(args[0]) : 0;
        double end = isArg(args, 1) ? ScriptRuntime.toNumber(args[1]) : self.getLength();
        int endI =
                ScriptRuntime.toInt32(
                        Math.max(
                                0,
                                Math.min(
                                        self.getLength(),
                                        (end < 0 ? self.getLength() + end : end))));
        int startI =
                ScriptRuntime.toInt32(
                        Math.min(
                                endI, Math.max(0, (start < 0 ? self.getLength() + start : start))));
        int len = endI - startI;

        Constructable constructor =
                AbstractEcmaObjectOperations.speciesConstructor(cx, thisObj, defaultConstructor);
        Scriptable newBuf = constructor.construct(cx, scope, new Object[] {len});
        if (!(newBuf instanceof NativeArrayBuffer)) {
            throw ScriptRuntime.typeErrorById("msg.species.invalid.ctor");
        }
        NativeArrayBuffer buf = (NativeArrayBuffer) newBuf;

        if (buf == self) {
            throw ScriptRuntime.typeErrorById("msg.arraybuf.same");
        }

        int actualLength = buf.getLength();
        if (actualLength < len) {
            throw ScriptRuntime.typeErrorById("msg.arraybuf.smaller.len", len, actualLength);
        }

        System.arraycopy(self.buffer, startI, buf.buffer, 0, len);
        return buf;
    }

    private static Object js_byteLength(Scriptable thisObj) {
        return getSelf(thisObj).getLength();
    }

    private static boolean isArg(Object[] args, int i) {
        return ((args.length > i) && !Undefined.instance.equals(args[i]));
    }
}
