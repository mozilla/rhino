/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import java.math.BigInteger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.SymbolKey;
import org.mozilla.javascript.Undefined;

/**
 * This class represents the JavaScript "DataView" interface, which allows direct manipulations of
 * the bytes in a NativeArrayBuffer. Java programmers would be best off getting the underling
 * "byte[]" array from the NativeArrayBuffer and manipulating it directly, perhaps using the
 * "ByteIo" class as a helper.
 */
public class NativeDataView extends NativeArrayBufferView {
    private static final long serialVersionUID = 1427967607557438968L;

    public static final String CLASS_NAME = "DataView";

    public NativeDataView() {
        super();
    }

    public NativeDataView(NativeArrayBuffer ab, int offset, int length) {
        super(ab, offset, length);
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    public static Object init(Context cx, Scriptable scope, boolean sealed) {
        LambdaConstructor constructor =
                new LambdaConstructor(
                        scope,
                        CLASS_NAME,
                        1,
                        LambdaConstructor.CONSTRUCTOR_NEW,
                        NativeDataView::js_constructor);
        constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);

        constructor.definePrototypeProperty(
                cx, "buffer", (Scriptable thisObj) -> realThis(thisObj).arrayBuffer);
        constructor.definePrototypeProperty(
                cx,
                "byteLength",
                (Scriptable thisObj) -> {
                    NativeDataView self = realThis(thisObj);
                    if (self.isDataViewOutOfBounds()) {
                        throw ScriptRuntime.typeErrorById("msg.dataview.bounds");
                    }
                    return self.byteLength;
                });
        constructor.definePrototypeProperty(
                cx,
                "byteOffset",
                (Scriptable thisObj) -> {
                    NativeDataView self = realThis(thisObj);
                    if (self.isDataViewOutOfBounds()) {
                        throw ScriptRuntime.typeErrorById("msg.dataview.bounds");
                    }
                    return self.offset;
                });

        constructor.definePrototypeProperty(
                SymbolKey.TO_STRING_TAG, CLASS_NAME, DONTENUM | READONLY);
        constructor.definePrototypeMethod(scope, "getFloat32", 1, NativeDataView::js_getFloat32);
        constructor.definePrototypeMethod(scope, "getFloat64", 1, NativeDataView::js_getFloat64);
        constructor.definePrototypeMethod(scope, "getInt8", 1, NativeDataView::js_getInt8);
        constructor.definePrototypeMethod(scope, "getInt16", 1, NativeDataView::js_getInt16);
        constructor.definePrototypeMethod(scope, "getInt32", 1, NativeDataView::js_getInt32);
        constructor.definePrototypeMethod(scope, "getUint8", 1, NativeDataView::js_getUint8);
        constructor.definePrototypeMethod(scope, "getUint16", 1, NativeDataView::js_getUint16);
        constructor.definePrototypeMethod(scope, "getUint32", 1, NativeDataView::js_getUint32);
        constructor.definePrototypeMethod(scope, "getBigInt64", 1, NativeDataView::js_getBigInt64);
        constructor.definePrototypeMethod(
                scope, "getBigUint64", 1, NativeDataView::js_getBigUint64);
        constructor.definePrototypeMethod(scope, "setFloat32", 2, NativeDataView::js_setFloat32);
        constructor.definePrototypeMethod(scope, "setFloat64", 2, NativeDataView::js_setFloat64);
        constructor.definePrototypeMethod(scope, "setInt8", 2, NativeDataView::js_setInt8);
        constructor.definePrototypeMethod(scope, "setInt16", 2, NativeDataView::js_setInt16);
        constructor.definePrototypeMethod(scope, "setInt32", 2, NativeDataView::js_setInt32);
        constructor.definePrototypeMethod(scope, "setUint8", 2, NativeDataView::js_setUint8);
        constructor.definePrototypeMethod(scope, "setUint16", 2, NativeDataView::js_setUint16);
        constructor.definePrototypeMethod(scope, "setUint32", 2, NativeDataView::js_setUint32);
        constructor.definePrototypeMethod(scope, "setBigInt64", 2, NativeDataView::js_setBigInt64);
        constructor.definePrototypeMethod(
                scope, "setBigUint64", 2, NativeDataView::js_setBigUint64);

        if (sealed) {
            constructor.sealObject();
            ((ScriptableObject) constructor.getPrototypeProperty()).sealObject();
        }
        return constructor;
    }

    private static NativeDataView realThis(Scriptable thisObj) {
        return LambdaConstructor.convertThisObject(thisObj, NativeDataView.class);
    }

    private static NativeDataView js_constructor(Context cx, Scriptable scope, Object[] args) {
        if (!isArg(args, 0) || !(args[0] instanceof NativeArrayBuffer)) {
            throw ScriptRuntime.constructError("TypeError", "Missing parameters");
        }

        NativeArrayBuffer ab = (NativeArrayBuffer) args[0];

        int pos = ScriptRuntime.toIndex(isArg(args, 1) ? args[1] : Undefined.instance);

        if (ab.isDetached()) {
            throw ScriptRuntime.typeErrorById("msg.arraybuf.detached");
        }

        int bufferByteLength = ab.getLength();
        if (pos > bufferByteLength) {
            throw ScriptRuntime.rangeErrorById("msg.dataview.offset.range");
        }

        int len;
        if (isArg(args, 2)) {
            len = ScriptRuntime.toIndex(args[2]);
            if ((long) pos + len > bufferByteLength) {
                throw ScriptRuntime.rangeErrorById("msg.dataview.length.range");
            }
        } else {
            len = bufferByteLength - pos;
        }

        if (ab.isDetached()) {
            throw ScriptRuntime.typeErrorById("msg.arraybuf.detached");
        }

        bufferByteLength = ab.getLength();
        if (pos > bufferByteLength) {
            throw ScriptRuntime.rangeErrorById("msg.dataview.offset.range");
        }

        if (isArg(args, 2)) {
            if ((long) pos + len > bufferByteLength) {
                throw ScriptRuntime.rangeErrorById("msg.dataview.length.range");
            }
        }

        return new NativeDataView(ab, pos, len);
    }

    private static Object js_getInt8(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        return realThis.js_getInt(cx, scope, 1, true, args);
    }

    private static Object js_getInt16(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        return realThis.js_getInt(cx, scope, 2, true, args);
    }

    private static Object js_getInt32(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        return realThis.js_getInt(cx, scope, 4, true, args);
    }

    private static Object js_getUint8(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        return realThis.js_getInt(cx, scope, 1, false, args);
    }

    private static Object js_getUint16(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        return realThis.js_getInt(cx, scope, 2, false, args);
    }

    private static Object js_getUint32(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        return realThis.js_getInt(cx, scope, 4, false, args);
    }

    private Object js_getInt(
            Context cx, Scriptable scope, int bytes, boolean signed, Object[] args) {
        int pos = ScriptRuntime.toIndex(isArg(args, 0) ? args[0] : Undefined.instance);

        boolean littleEndian = isArg(args, 1) && (bytes > 1) && ScriptRuntime.toBoolean(args[1]);

        if (isDataViewOutOfBounds()) {
            throw ScriptRuntime.typeErrorById("msg.dataview.bounds");
        }

        int viewSize = byteLength;
        if ((long) pos + bytes > viewSize) {
            throw ScriptRuntime.rangeErrorById("msg.dataview.offset.range");
        }

        switch (bytes) {
            case 1:
                if (signed) {
                    return ByteIo.readInt8(arrayBuffer.buffer, offset + pos);
                } else {
                    return ByteIo.readUint8(arrayBuffer.buffer, offset + pos);
                }
            case 2:
                if (signed) {
                    return ByteIo.readInt16(arrayBuffer.buffer, offset + pos, littleEndian);
                } else {
                    return ByteIo.readUint16(arrayBuffer.buffer, offset + pos, littleEndian);
                }
            case 4:
                return signed
                        ? ByteIo.readInt32(arrayBuffer.buffer, offset + pos, littleEndian)
                        : ByteIo.readUint32(arrayBuffer.buffer, offset + pos, littleEndian);
            default:
                throw new AssertionError();
        }
    }

    private static Object js_getFloat32(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        return realThis.js_getFloat(cx, scope, 4, args);
    }

    private static Object js_getFloat64(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        return realThis.js_getFloat(cx, scope, 8, args);
    }

    private Object js_getFloat(Context cx, Scriptable scope, int bytes, Object[] args) {
        int pos = ScriptRuntime.toIndex(isArg(args, 0) ? args[0] : Undefined.instance);

        boolean littleEndian = isArg(args, 1) && (bytes > 1) && ScriptRuntime.toBoolean(args[1]);

        if (isDataViewOutOfBounds()) {
            throw ScriptRuntime.typeErrorById("msg.dataview.bounds");
        }

        int viewSize = byteLength;
        if ((long) pos + bytes > viewSize) {
            throw ScriptRuntime.rangeErrorById("msg.dataview.offset.range");
        }

        switch (bytes) {
            case 4:
                return ByteIo.readFloat32(arrayBuffer.buffer, offset + pos, littleEndian);
            case 8:
                return ByteIo.readFloat64(arrayBuffer.buffer, offset + pos, littleEndian);
            default:
                throw new AssertionError();
        }
    }

    private static Object js_setInt8(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        realThis.js_setInt(cx, scope, 1, true, args);
        return Undefined.instance;
    }

    private static Object js_setInt16(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        realThis.js_setInt(cx, scope, 2, true, args);
        return Undefined.instance;
    }

    private static Object js_setInt32(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        realThis.js_setInt(cx, scope, 4, true, args);
        return Undefined.instance;
    }

    private static Object js_setUint8(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        realThis.js_setInt(cx, scope, 1, false, args);
        return Undefined.instance;
    }

    private static Object js_setUint16(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        realThis.js_setInt(cx, scope, 2, false, args);
        return Undefined.instance;
    }

    private static Object js_setUint32(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        realThis.js_setInt(cx, scope, 4, false, args);
        return Undefined.instance;
    }

    private void js_setInt(Context cx, Scriptable scope, int bytes, boolean signed, Object[] args) {
        int pos = ScriptRuntime.toIndex(isArg(args, 0) ? args[0] : Undefined.instance);

        Object val = isArg(args, 1) ? ScriptRuntime.toNumber(args[1]) : ScriptRuntime.zeroObj;

        boolean littleEndian = isArg(args, 2) && (bytes > 1) && ScriptRuntime.toBoolean(args[2]);

        if (isDataViewOutOfBounds()) {
            throw ScriptRuntime.typeErrorById("msg.dataview.bounds");
        }

        int viewSize = byteLength;
        if ((long) pos + bytes > viewSize) {
            throw ScriptRuntime.rangeErrorById("msg.dataview.offset.range");
        }

        switch (bytes) {
            case 1:
                if (signed) {
                    int value = Conversions.toInt8(val);
                    if (pos + bytes > byteLength) {
                        throw ScriptRuntime.rangeErrorById("msg.dataview.offset.range");
                    }
                    ByteIo.writeInt8(arrayBuffer.buffer, offset + pos, value);
                } else {
                    int value = Conversions.toUint8(val);
                    if (pos + bytes > byteLength) {
                        throw ScriptRuntime.rangeErrorById("msg.dataview.offset.range");
                    }
                    ByteIo.writeUint8(arrayBuffer.buffer, offset + pos, value);
                }
                break;
            case 2:
                if (signed) {
                    int value = Conversions.toInt16(val);
                    if (pos + bytes > byteLength) {
                        throw ScriptRuntime.rangeErrorById("msg.dataview.offset.range");
                    }
                    ByteIo.writeInt16(arrayBuffer.buffer, offset + pos, value, littleEndian);
                } else {
                    int value = Conversions.toUint16(val);
                    if (pos + bytes > byteLength) {
                        throw ScriptRuntime.rangeErrorById("msg.dataview.offset.range");
                    }
                    ByteIo.writeUint16(arrayBuffer.buffer, offset + pos, value, littleEndian);
                }
                break;
            case 4:
                if (signed) {
                    int value = Conversions.toInt32(val);
                    if (pos + bytes > byteLength) {
                        throw ScriptRuntime.rangeErrorById("msg.dataview.offset.range");
                    }
                    ByteIo.writeInt32(arrayBuffer.buffer, offset + pos, value, littleEndian);
                } else {
                    long value = Conversions.toUint32(val);
                    if (pos + bytes > byteLength) {
                        throw ScriptRuntime.rangeErrorById("msg.dataview.offset.range");
                    }
                    ByteIo.writeUint32(arrayBuffer.buffer, offset + pos, value, littleEndian);
                }
                break;
            default:
                throw new AssertionError();
        }
    }

    private static Object js_setFloat32(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        realThis.js_setFloat(cx, scope, 4, args);
        return Undefined.instance;
    }

    private static Object js_setFloat64(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        realThis.js_setFloat(cx, scope, 8, args);
        return Undefined.instance;
    }

    private void js_setFloat(Context cx, Scriptable scope, int bytes, Object[] args) {
        int pos = ScriptRuntime.toIndex(isArg(args, 0) ? args[0] : Undefined.instance);

        double val = isArg(args, 1) ? ScriptRuntime.toNumber(args[1]) : Double.NaN;

        boolean littleEndian = isArg(args, 2) && (bytes > 1) && ScriptRuntime.toBoolean(args[2]);

        if (isDataViewOutOfBounds()) {
            throw ScriptRuntime.typeErrorById("msg.dataview.bounds");
        }

        int viewSize = byteLength;
        if ((long) pos + bytes > viewSize) {
            throw ScriptRuntime.rangeErrorById("msg.dataview.offset.range");
        }

        switch (bytes) {
            case 4:
                ByteIo.writeFloat32(arrayBuffer.buffer, offset + pos, val, littleEndian);
                break;
            case 8:
                ByteIo.writeFloat64(arrayBuffer.buffer, offset + pos, val, littleEndian);
                break;
            default:
                throw new AssertionError();
        }
    }

    private static Object js_getBigInt64(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        return realThis.js_getBigInt(true, args);
    }

    private static Object js_getBigUint64(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        return realThis.js_getBigInt(false, args);
    }

    private Object js_getBigInt(boolean signed, Object[] args) {
        int pos = ScriptRuntime.toIndex(isArg(args, 0) ? args[0] : Undefined.instance);

        boolean littleEndian = isArg(args, 1) && ScriptRuntime.toBoolean(args[1]);

        if (isDataViewOutOfBounds()) {
            throw ScriptRuntime.typeErrorById("msg.dataview.bounds");
        }

        int viewSize = byteLength;
        if ((long) pos + 8 > viewSize) {
            throw ScriptRuntime.rangeErrorById("msg.dataview.offset.range");
        }

        long base = ByteIo.readUint64Primitive(arrayBuffer.buffer, offset + pos, littleEndian);

        if (signed) {
            // Interpret as signed 64-bit integer
            return BigInteger.valueOf(base);
        } else {
            // Interpret as unsigned 64-bit integer
            if (base >= 0L) {
                return BigInteger.valueOf(base);
            } else {
                // Split into two 32-bit parts for proper unsigned conversion
                var lsw = BigInteger.valueOf(base & 0xffffffffL);
                var msw = BigInteger.valueOf((base >>> 32)).shiftLeft(32);
                return msw.add(lsw);
            }
        }
    }

    // These two functions are specified separately, and sound like they should behave differently,
    // but end up having precisely the same effect, with only the interpretation of the 64 bits in
    // an intermediate state differing.
    private static Object js_setBigInt64(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        realThis.js_setBigInt(args);
        return Undefined.instance;
    }

    private static Object js_setBigUint64(
            Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        realThis.js_setBigInt(args);
        return Undefined.instance;
    }

    private void js_setBigInt(Object[] args) {
        int pos = ScriptRuntime.toIndex(isArg(args, 0) ? args[0] : Undefined.instance);

        BigInteger val = ScriptRuntime.toBigInt(isArg(args, 1) ? args[1] : Undefined.instance);

        boolean littleEndian = isArg(args, 2) && ScriptRuntime.toBoolean(args[2]);

        if (isDataViewOutOfBounds()) {
            throw ScriptRuntime.typeErrorById("msg.dataview.bounds");
        }

        int viewSize = byteLength;
        if ((long) pos + 8 > viewSize) {
            throw ScriptRuntime.rangeErrorById("msg.dataview.offset.range");
        }

        long base = val.longValue();
        ByteIo.writeUint64(arrayBuffer.buffer, offset + pos, base, littleEndian);
    }

    public boolean isDataViewOutOfBounds() {
        if (arrayBuffer.isDetached()) {
            return true;
        }

        int bufferByteLength = arrayBuffer.getLength();

        int byteOffsetStart = offset;
        int byteOffsetEnd = offset + byteLength;

        return byteOffsetStart > bufferByteLength || byteOffsetEnd > bufferByteLength;
    }
}
