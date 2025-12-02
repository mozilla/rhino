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
        double length = isArg(args, 0) ? ScriptRuntime.toNumber(args[0]) : 0;

        // ES2024: Check for options parameter with maxByteLength
        int maxByteLength = -1;
        if (isArg(args, 1) && args[1] instanceof Scriptable) {
            Scriptable options = (Scriptable) args[1];
            Object maxByteLengthValue = ScriptableObject.getProperty(options, "maxByteLength");
            if (maxByteLengthValue != Scriptable.NOT_FOUND
                    && !Undefined.instance.equals(maxByteLengthValue)) {
                double maxLen = ScriptRuntime.toNumber(maxByteLengthValue);

                // Validate maxByteLength
                if (Double.isNaN(maxLen)) {
                    maxLen = 0;
                }
                if (maxLen < 0 || Double.isInfinite(maxLen)) {
                    throw ScriptRuntime.rangeError("Invalid maxByteLength");
                }
                if (maxLen >= Integer.MAX_VALUE) {
                    throw ScriptRuntime.rangeError("maxByteLength too large");
                }

                int maxLenInt = (int) maxLen;

                // Check that length <= maxByteLength
                if (length > maxLenInt) {
                    throw ScriptRuntime.rangeError("ArrayBuffer length exceeds maxByteLength");
                }

                maxByteLength = maxLenInt;
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

    // ES2025 ArrayBuffer.prototype.transfer
    private static Scriptable js_transfer(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeArrayBuffer self = getSelf(thisObj);

        // 1. Perform ? RequireInternalSlot(O, [[ArrayBufferData]])
        // 2. If IsSharedArrayBuffer(O) is true, throw a TypeError exception
        // (Rhino doesn't support SharedArrayBuffer, so this is implicit)

        // 3. If IsDetachedBuffer(O) is true, throw a TypeError exception
        if (self.isDetached()) {
            throw ScriptRuntime.typeErrorById("msg.arraybuf.detached");
        }

        // 4. If newLength is undefined, let newByteLength be O.[[ArrayBufferByteLength]]
        // 5. Else, let newByteLength be ? ToIntegerOrInfinity(newLength)
        int newByteLength = validateNewByteLength(args, self.getLength());

        // 6. Let new be ? Construct(%ArrayBuffer%, « 𝔽(newByteLength) »)
        Constructable constructor =
                AbstractEcmaObjectOperations.speciesConstructor(
                        cx,
                        thisObj,
                        TopLevel.getBuiltinCtor(
                                cx,
                                ScriptableObject.getTopLevelScope(scope),
                                TopLevel.Builtins.ArrayBuffer));
        Scriptable newBuf = constructor.construct(cx, scope, new Object[] {newByteLength});
        if (!(newBuf instanceof NativeArrayBuffer)) {
            throw ScriptRuntime.typeErrorById("msg.species.invalid.ctor");
        }
        NativeArrayBuffer newBuffer = (NativeArrayBuffer) newBuf;

        // 7. Let copyLength be min(newByteLength, O.[[ArrayBufferByteLength]])
        int copyLength = Math.min(newByteLength, self.getLength());

        // 8-11. Copy data from old buffer to new buffer
        if (copyLength > 0) {
            System.arraycopy(self.buffer, 0, newBuffer.buffer, 0, copyLength);
        }

        // 12. Perform ! DetachArrayBuffer(O)
        self.detach();

        // 13. Return new
        return newBuf;
    }

    // ES2025 ArrayBuffer.prototype.transferToFixedLength
    private static Scriptable js_transferToFixedLength(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeArrayBuffer self = getSelf(thisObj);

        // 1. Let O be the this value
        // 2. Perform ? RequireInternalSlot(O, [[ArrayBufferData]])
        // (getSelf handles this validation)

        // 3. If IsSharedArrayBuffer(O) is true, throw a TypeError exception
        // (Rhino doesn't support SharedArrayBuffer, so this is implicit)

        // 4. If IsDetachedBuffer(O) is true, throw a TypeError exception
        if (self.isDetached()) {
            throw ScriptRuntime.typeErrorById("msg.arraybuf.detached");
        }

        // 5. If newLength is undefined, let newByteLength be O.[[ArrayBufferByteLength]]
        // 6. Else, let newByteLength be ? ToIntegerOrInfinity(newLength)
        // 7. If newByteLength < 0 or newByteLength is +∞, throw a RangeError exception
        int newByteLength = validateNewByteLength(args, self.getLength());

        // 8. Let new be ? Construct(%ArrayBuffer%, « 𝔽(newByteLength) »)
        // Note: This creates a fixed-length buffer (no maxByteLength parameter)
        Constructable constructor =
                AbstractEcmaObjectOperations.speciesConstructor(
                        cx,
                        thisObj,
                        TopLevel.getBuiltinCtor(
                                cx,
                                ScriptableObject.getTopLevelScope(scope),
                                TopLevel.Builtins.ArrayBuffer));
        Scriptable newBuf = constructor.construct(cx, scope, new Object[] {newByteLength});

        // 9. NOTE: This method returns a fixed-length ArrayBuffer
        // 10. If new.[[ArrayBufferDetachKey]] is not undefined, throw a TypeError exception
        if (!(newBuf instanceof NativeArrayBuffer)) {
            throw ScriptRuntime.typeErrorById("msg.species.invalid.ctor");
        }
        NativeArrayBuffer newBuffer = (NativeArrayBuffer) newBuf;

        // 11. Let copyLength be min(newByteLength, O.[[ArrayBufferByteLength]])
        int copyLength = Math.min(newByteLength, self.getLength());

        // 12. Let fromBlock be O.[[ArrayBufferData]]
        // 13. Let toBlock be new.[[ArrayBufferData]]
        // 14. Perform CopyDataBlockBytes(toBlock, 0, fromBlock, 0, copyLength)
        // 15. NOTE: Neither creation of the new ArrayBuffer nor copying from the old
        //     ArrayBuffer are observable. Implementations may implement this method
        //     as a zero-copy move or a realloc
        if (copyLength > 0) {
            System.arraycopy(self.buffer, 0, newBuffer.buffer, 0, copyLength);
        }

        // 16. Perform ! DetachArrayBuffer(O)
        self.detach();

        // 17. Return new
        return newBuf;
    }

    private static boolean isArg(Object[] args, int i) {
        return ((args.length > i) && !Undefined.instance.equals(args[i]));
    }

    /**
     * Validates and converts the newLength parameter for transfer operations. Implements
     * ToIntegerOrInfinity conversion and range validation.
     *
     * @param args the arguments array
     * @param defaultLength the default length if no argument is provided
     * @return the validated byte length as an integer
     * @throws RangeError if the length is invalid
     */
    private static int validateNewByteLength(Object[] args, int defaultLength) {
        double newLength = isArg(args, 0) ? ScriptRuntime.toNumber(args[0]) : defaultLength;

        // ToIntegerOrInfinity: Handle NaN (convert to 0)
        if (Double.isNaN(newLength)) {
            newLength = 0;
        }

        // Check for negative or infinite values
        if (newLength < 0 || Double.isInfinite(newLength)) {
            throw ScriptRuntime.rangeError("Invalid array buffer length");
        }

        // Check for values too large for Java arrays
        if (newLength >= Integer.MAX_VALUE) {
            throw ScriptRuntime.rangeError("Array buffer length too large");
        }

        return (int) newLength;
    }

    // ES2024 ArrayBuffer.prototype.resize
    private static Object js_resize(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeArrayBuffer self = getSelf(thisObj);

        // 1. Let O be the this value
        // 2. Perform ? RequireInternalSlot(O, [[ArrayBufferData]])
        // (getSelf handles this validation)

        // 3. If IsDetachedBuffer(O) is true, throw a TypeError exception
        if (self.isDetached()) {
            throw ScriptRuntime.typeErrorById("msg.arraybuf.detached");
        }

        // 4. If IsResizableArrayBuffer(O) is false, throw a TypeError exception
        if (self.maxByteLength < 0) {
            throw ScriptRuntime.typeError("ArrayBuffer is not resizable");
        }

        // 5. Let newByteLength be ? ToIntegerOrInfinity(newLength)
        if (!isArg(args, 0)) {
            throw ScriptRuntime.typeError("Missing required argument: newLength");
        }

        double newLengthDouble = ScriptRuntime.toNumber(args[0]);

        // ToIntegerOrInfinity: Handle NaN (convert to 0)
        if (Double.isNaN(newLengthDouble)) {
            newLengthDouble = 0;
        }

        // 6. If newByteLength < 0 or newByteLength > O.[[ArrayBufferMaxByteLength]], throw a
        // RangeError exception
        if (newLengthDouble < 0 || Double.isInfinite(newLengthDouble)) {
            throw ScriptRuntime.rangeError("Invalid newLength");
        }

        if (newLengthDouble >= Integer.MAX_VALUE) {
            throw ScriptRuntime.rangeError("newLength too large");
        }

        int newLength = (int) newLengthDouble;

        if (newLength > self.maxByteLength) {
            throw ScriptRuntime.rangeError(
                    "newLength exceeds maxByteLength (" + self.maxByteLength + ")");
        }

        // 7. Let hostHandled be ? HostResizeArrayBuffer(O, newByteLength)
        // 8. If hostHandled is handled, return undefined
        // 9. Let oldBlock be O.[[ArrayBufferData]]
        // 10. Let newBlock be ? CreateByteDataBlock(newByteLength)
        // 11. Let copyLength be min(newByteLength, O.[[ArrayBufferByteLength]])
        // 12. Perform CopyDataBlockBytes(newBlock, 0, oldBlock, 0, copyLength)
        // 13. NOTE: Neither creation of the new Data Block nor copying from the old Data Block
        // are observable
        // 14. Set O.[[ArrayBufferData]] to newBlock
        // 15. Set O.[[ArrayBufferByteLength]] to newByteLength

        int oldLength = self.getLength();
        if (newLength == oldLength) {
            // No resize needed
            return Undefined.instance;
        }

        byte[] newBuffer = new byte[newLength];
        int copyLength = Math.min(newLength, oldLength);

        // Copy existing data
        if (copyLength > 0) {
            System.arraycopy(self.buffer, 0, newBuffer, 0, copyLength);
        }

        // New bytes are automatically initialized to 0 in Java
        self.buffer = newBuffer;

        // 16. Return undefined
        return Undefined.instance;
    }

    // ES2024 ArrayBuffer.prototype.resizable getter
    private static Object js_resizable(Scriptable thisObj) {
        NativeArrayBuffer self = getSelf(thisObj);
        // A buffer is resizable if maxByteLength was specified in constructor
        return self.maxByteLength >= 0;
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
