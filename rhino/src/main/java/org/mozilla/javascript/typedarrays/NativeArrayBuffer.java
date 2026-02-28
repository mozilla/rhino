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
import org.mozilla.javascript.SymbolKey;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.Undefined;

/**
 * A NativeArrayBuffer is the backing buffer for a typed array. Used inside JavaScript code, it
 * implements the ArrayBuffer interface. Used directly from Java, it simply holds a byte array.
 */
public class NativeArrayBuffer extends ScriptableObject {
    private static final long serialVersionUID = 3110411773054879549L;

    public static final String CLASS_NAME = "ArrayBuffer";

    private static final byte[] EMPTY_BUF = new byte[0];

    byte[] buffer;
    // ES2024: maxByteLength for resizable buffers (-1 = fixed-length)
    private int maxByteLength = -1;

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

        constructor.defineConstructorMethod(scope, "isView", 1, NativeArrayBuffer::js_isView);
        constructor.definePrototypeMethod(scope, "slice", 2, NativeArrayBuffer::js_slice);
        constructor.definePrototypeMethod(scope, "transfer", 0, NativeArrayBuffer::js_transfer);
        constructor.definePrototypeMethod(
                scope, "transferToFixedLength", 0, NativeArrayBuffer::js_transferToFixedLength);
        constructor.definePrototypeMethod(scope, "resize", 1, NativeArrayBuffer::js_resize);
        constructor.definePrototypeProperty(cx, "byteLength", NativeArrayBuffer::js_byteLength);
        constructor.definePrototypeProperty(cx, "detached", NativeArrayBuffer::js_detached);
        constructor.definePrototypeProperty(cx, "resizable", NativeArrayBuffer::js_resizable);
        constructor.definePrototypeProperty(
                cx, "maxByteLength", NativeArrayBuffer::js_maxByteLength);
        constructor.definePrototypeProperty(
                SymbolKey.TO_STRING_TAG, "ArrayBuffer", DONTENUM | READONLY);

        if (sealed) {
            constructor.sealObject();
            ((ScriptableObject) constructor.getPrototypeProperty()).sealObject();
        }
        return constructor;
    }

    /** Create an empty buffer. */
    public NativeArrayBuffer() {
        buffer = EMPTY_BUF;
    }

    /** Create a buffer of the specified length in bytes. */
    public NativeArrayBuffer(double len) {
        this(ScriptRuntime.toIndex(len));
    }

    private NativeArrayBuffer(int len) {
        if (len == 0) {
            buffer = EMPTY_BUF;
        } else {
            try {
                buffer = new byte[len];
            } catch (OutOfMemoryError e) {
                throw ScriptRuntime.rangeErrorById("msg.arraybuf.oom");
            }
        }
    }

    /** Get the number of bytes in the buffer. */
    public int getLength() {
        return buffer != null ? buffer.length : 0;
    }

    /**
     * Return the actual bytes that back the buffer. This is a reference to the real buffer, so
     * changes to bytes here will be reflected in the actual object and all its views.
     */
    public byte[] getBuffer() {
        return buffer;
    }

    public void detach() {
        buffer = null;
    }

    public boolean isDetached() {
        return buffer == null;
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
                        Math.max(0, Math.min(getLength(), (e < 0 ? getLength() + e : e))));
        int start =
                ScriptRuntime.toInt32(Math.min(end, Math.max(0, (s < 0 ? getLength() + s : s))));
        int len = end - start;

        NativeArrayBuffer newBuf = new NativeArrayBuffer(len);
        System.arraycopy(buffer, start, newBuf.buffer, 0, len);
        return newBuf;
    }

    private static NativeArrayBuffer getSelf(Scriptable thisObj) {
        return LambdaConstructor.convertThisObject(thisObj, NativeArrayBuffer.class);
    }

    private static NativeArrayBuffer js_constructor(Context cx, Scriptable scope, Object[] args) {
        double length = isArg(args, 0) ? ScriptRuntime.toIndex(args[0]) : 0;

        // ES2024: Check for options parameter with maxByteLength
        int maxByteLength = -1;
        if (isArg(args, 1) && args[1] instanceof Scriptable) {
            Scriptable options = (Scriptable) args[1];
            Object maxByteLengthValue = ScriptableObject.getProperty(options, "maxByteLength");
            if (maxByteLengthValue != Scriptable.NOT_FOUND
                    && !Undefined.isUndefined(maxByteLengthValue)) {
                maxByteLength = ScriptRuntime.toIndex(maxByteLengthValue);
                if (length > maxByteLength) {
                    throw ScriptRuntime.rangeErrorById("msg.arraybuf.range.mismatch");
                }
                if (maxByteLength > Runtime.getRuntime().maxMemory()) {
                    // Sanity check (in the 262 tests) to avoid an impossibly-large maximum
                    throw ScriptRuntime.rangeErrorById("msg.arraybuf.range.toobig");
                }
            }
        }

        NativeArrayBuffer buffer = new NativeArrayBuffer(length);
        buffer.maxByteLength = maxByteLength;
        return buffer;
    }

    private static Boolean js_isView(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return Boolean.valueOf((isArg(args, 0) && (args[0] instanceof NativeArrayBufferView)));
    }

    private static NativeArrayBuffer js_slice(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeArrayBuffer self = getSelf(thisObj);

        if (self.isDetached()) {
            throw ScriptRuntime.typeErrorById("msg.arraybuf.detached");
        }

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
                AbstractEcmaObjectOperations.speciesConstructor(
                        cx,
                        thisObj,
                        TopLevel.getBuiltinCtor(
                                cx,
                                ScriptableObject.getTopLevelScope(scope),
                                TopLevel.Builtins.ArrayBuffer));
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

    private static Object js_detached(Scriptable thisObj) {
        return getSelf(thisObj).isDetached();
    }

    private NativeArrayBuffer copyAndDetach(
            Context cx, Scriptable scope, Object lenObj, boolean preserveResizability) {
        int newLength;
        if (Undefined.isUndefined(lenObj)) {
            newLength = getLength();
        } else {
            newLength = ScriptRuntime.toIndex(lenObj);
        }
        if (isDetached()) {
            throw ScriptRuntime.typeErrorById("msg.arraybuf.detached");
        }

        var arg2 = cx.newObject(scope);
        if (preserveResizability && maxByteLength >= 0) {
            arg2.put("maxByteLength", arg2, maxByteLength);
        }

        var constructor =
                AbstractEcmaObjectOperations.speciesConstructor(
                        cx,
                        this,
                        TopLevel.getBuiltinCtor(
                                cx,
                                ScriptableObject.getTopLevelScope(scope),
                                TopLevel.Builtins.ArrayBuffer));
        var newBuf = constructor.construct(cx, scope, new Object[] {newLength, arg2});
        if (!(newBuf instanceof NativeArrayBuffer)) {
            throw ScriptRuntime.typeErrorById("msg.species.invalid.ctor");
        }
        var newBuffer = (NativeArrayBuffer) newBuf;
        int copyLength = Math.min(newLength, getLength());
        if (copyLength > 0) {
            System.arraycopy(buffer, 0, newBuffer.buffer, 0, copyLength);
        }
        detach();
        return newBuffer;
    }

    // ES2025 ArrayBuffer.prototype.transfer
    private static Scriptable js_transfer(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        var self = getSelf(thisObj);
        var arg1 = args.length > 0 ? args[0] : Undefined.instance;
        return self.copyAndDetach(cx, scope, arg1, true);
    }

    // ES2025 ArrayBuffer.prototype.transferToFixedLength
    private static Scriptable js_transferToFixedLength(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        var self = getSelf(thisObj);
        var arg1 = args.length > 0 ? args[0] : Undefined.instance;
        return self.copyAndDetach(cx, scope, arg1, false);
    }

    private static boolean isArg(Object[] args, int i) {
        return ((args.length > i) && !Undefined.instance.equals(args[i]));
    }

    // ES2024 ArrayBuffer.prototype.resize
    private static Object js_resize(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeArrayBuffer self = getSelf(thisObj);
        if (!self.isResizable()) {
            throw ScriptRuntime.typeErrorById("msg.arraybuf.notresizeable");
        }
        var arg1 = args.length > 0 ? args[0] : Undefined.instance;
        int newLength = ScriptRuntime.toIndex(arg1);
        if (self.isDetached()) {
            throw ScriptRuntime.typeErrorById("msg.arraybuf.detached");
        }
        if (newLength > self.maxByteLength) {
            throw ScriptRuntime.rangeErrorById("msg.arraybuf.range.exceedsmax", self.maxByteLength);
        }
        int oldLength = self.getLength();
        if (newLength == oldLength) {
            // No resize needed
            return Undefined.instance;
        }

        byte[] newBuffer = new byte[newLength];
        int copyLength = Math.min(newLength, oldLength);

        if (copyLength > 0) {
            System.arraycopy(self.buffer, 0, newBuffer, 0, copyLength);
        }

        // New bytes are automatically initialized to 0 in Java
        self.buffer = newBuffer;
        return Undefined.instance;
    }

    /** Return true if this ArrayBuffer is resizable (ES2024). */
    public boolean isResizable() {
        return maxByteLength >= 0;
    }

    // ES2024 ArrayBuffer.prototype.resizable getter
    private static Object js_resizable(Scriptable thisObj) {
        NativeArrayBuffer self = getSelf(thisObj);
        // A buffer is resizable if maxByteLength was specified in constructor
        return self.isResizable();
    }

    // ES2024 ArrayBuffer.prototype.maxByteLength getter
    private static Object js_maxByteLength(Scriptable thisObj) {
        NativeArrayBuffer self = getSelf(thisObj);
        // For fixed-length buffers, maxByteLength = byteLength
        // For resizable buffers, return the maxByteLength
        if (self.maxByteLength >= 0) {
            return self.maxByteLength;
        } else {
            return self.getLength();
        }
    }
}
