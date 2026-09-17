/* -*- Mode: java; tab-width: 8g; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import static org.mozilla.javascript.ClassDescriptor.Builder.value;
import static org.mozilla.javascript.ClassDescriptor.Destination.PROTO;
import static org.mozilla.javascript.SymbolKey.TO_STRING_TAG;

import java.io.Serial;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.mozilla.javascript.ClassDescriptor;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JSFunction;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.VarScope;

/**
 * This class represents the JavaScript "DataView" interface, which allows direct manipulations of
 * the bytes in a NativeArrayBuffer. Java programmers would be best off getting the {@link
 * java.nio.ByteBuffer} from the NativeArrayBuffer (via {@link NativeArrayBuffer#getByteBuffer}) and
 * manipulating it directly.
 */
public class NativeDataView extends NativeArrayBufferView {
    @Serial private static final long serialVersionUID = 1427967607557438968L;

    public static final String CLASS_NAME = "DataView";

    public static final ClassDescriptor DESCRIPTOR;

    static {
        DESCRIPTOR =
                new ClassDescriptor.Builder(
                                CLASS_NAME,
                                1,
                                NativeTypedArrayView::typeError,
                                NativeDataView::js_constructor)
                        .withProp(PROTO, "buffer", NativeDataView::js_buffer, null, DONTENUM)
                        .withProp(
                                PROTO, "byteLength", NativeDataView::js_byteLength, null, DONTENUM)
                        .withProp(
                                PROTO, "byteOffset", NativeDataView::js_byteOffset, null, DONTENUM)
                        .withProp(PROTO, TO_STRING_TAG, value(CLASS_NAME, DONTENUM | READONLY))
                        .withMethod(PROTO, "getFloat16", 1, NativeDataView::js_getFloat16)
                        .withMethod(PROTO, "getFloat32", 1, NativeDataView::js_getFloat32)
                        .withMethod(PROTO, "getFloat64", 1, NativeDataView::js_getFloat64)
                        .withMethod(PROTO, "getInt8", 1, NativeDataView::js_getInt8)
                        .withMethod(PROTO, "getInt16", 1, NativeDataView::js_getInt16)
                        .withMethod(PROTO, "getInt32", 1, NativeDataView::js_getInt32)
                        .withMethod(PROTO, "getUint8", 1, NativeDataView::js_getUint8)
                        .withMethod(PROTO, "getUint16", 1, NativeDataView::js_getUint16)
                        .withMethod(PROTO, "getUint32", 1, NativeDataView::js_getUint32)
                        .withMethod(PROTO, "getBigInt64", 1, NativeDataView::js_getBigInt64)
                        .withMethod(PROTO, "getBigUint64", 1, NativeDataView::js_getBigUint64)
                        .withMethod(PROTO, "setFloat16", 2, NativeDataView::js_setFloat16)
                        .withMethod(PROTO, "setFloat32", 2, NativeDataView::js_setFloat32)
                        .withMethod(PROTO, "setFloat64", 2, NativeDataView::js_setFloat64)
                        .withMethod(PROTO, "setInt8", 2, NativeDataView::js_setInt8)
                        .withMethod(PROTO, "setInt16", 2, NativeDataView::js_setInt16)
                        .withMethod(PROTO, "setInt32", 2, NativeDataView::js_setInt32)
                        .withMethod(PROTO, "setUint8", 2, NativeDataView::js_setUint8)
                        .withMethod(PROTO, "setUint16", 2, NativeDataView::js_setUint16)
                        .withMethod(PROTO, "setUint32", 2, NativeDataView::js_setUint32)
                        .withMethod(PROTO, "setBigInt64", 2, NativeDataView::js_setBigInt64)
                        .withMethod(PROTO, "setBigUint64", 2, NativeDataView::js_setBigUint64)
                        .build();
    }

    private final boolean autoLength;

    public NativeDataView() {
        super();
        this.autoLength = false;
    }

    private NativeDataView(NativeArrayBuffer ab, int offset, int length, boolean autoLength) {
        super(ab, offset, length);
        this.autoLength = autoLength;
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    public static Object init(Context cx, VarScope scope, boolean sealed) {
        return DESCRIPTOR.buildConstructor(cx, scope, new NativeObject(), sealed);
    }

    private static NativeDataView realThis(Object thisObj) {
        return LambdaConstructor.convertThisObject(thisObj, NativeDataView.class);
    }

    // A view of the buffer with the requested byte order. Duplicate is used so that setting the
    // order does not mutate the shared buffer, which would be unsafe for concurrent access.
    private ByteBuffer orderedView(boolean littleEndian) {
        return arrayBuffer
                .buffer
                .duplicate()
                .order(littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
    }

    private static boolean littleEndian(int arg, Object[] args) {
        if (arg >= args.length) {
            return false;
        }
        return ScriptRuntime.toBoolean(args[arg]);
    }

    private static Object js_buffer(Scriptable thisObj) {
        return realThis(thisObj).arrayBuffer;
    }

    private static Object js_byteLength(Scriptable thisObj) {
        NativeDataView self = realThis(thisObj);
        if (self.isDataViewOutOfBounds()) {
            throw ScriptRuntime.typeErrorById("msg.dataview.bounds");
        }
        return self.getByteLength();
    }

    private static Object js_byteOffset(Scriptable thisObj) {
        NativeDataView self = realThis(thisObj);
        if (self.isDataViewOutOfBounds()) {
            throw ScriptRuntime.typeErrorById("msg.dataview.bounds");
        }
        return self.offset;
    }

    private static NativeDataView js_constructor(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
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

        int len = 0;
        boolean autoLength = false;
        if (isArg(args, 2)) {
            len = ScriptRuntime.toIndex(args[2]);
            if ((long) pos + len > bufferByteLength) {
                throw ScriptRuntime.rangeErrorById("msg.dataview.length.range");
            }
        } else {
            if (ab.isResizable()) {
                autoLength = true;
            } else {
                len = bufferByteLength - pos;
            }
        }

        var res = new NativeDataView(ab, pos, len, autoLength);
        ScriptRuntime.setBuiltinProtoAndParent(res, f, nt, s, TopLevel.Builtins.DataView);
        return res;
    }

    @Override
    public int getByteLength() {
        if (!autoLength) {
            return byteLength;
        }
        return arrayBuffer.getLength() - offset;
    }

    private static Object js_getInt8(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        return realThis.js_getInt(cx, s, 1, true, args);
    }

    private static Object js_getInt16(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        return realThis.js_getInt(cx, s, 2, true, args);
    }

    private static Object js_getInt32(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        return realThis.js_getInt(cx, s, 4, true, args);
    }

    private static Object js_getUint8(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        return realThis.js_getInt(cx, s, 1, false, args);
    }

    private static Object js_getUint16(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        return realThis.js_getInt(cx, s, 2, false, args);
    }

    private static Object js_getUint32(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        return realThis.js_getInt(cx, s, 4, false, args);
    }

    private Object js_getInt(Context cx, VarScope scope, int bytes, boolean signed, Object[] args) {
        int pos = ScriptRuntime.toIndex(isArg(args, 0) ? args[0] : Undefined.instance);

        if (isDataViewOutOfBounds()) {
            throw ScriptRuntime.typeErrorById("msg.dataview.bounds");
        }

        int viewSize = getByteLength();
        if ((long) pos + bytes > viewSize) {
            throw ScriptRuntime.rangeErrorById("msg.dataview.offset.range");
        }

        switch (bytes) {
            case 1:
                int byteBits = arrayBuffer.buffer.get(offset + pos);
                if (signed) {
                    return (byte) byteBits;
                }
                return Conversions.byteBitsToUint(byteBits);
            case 2:
                short shortBits = orderedView(littleEndian(1, args)).getShort(offset + pos);
                if (signed) {
                    return shortBits;
                }
                return Conversions.shortBitsToUint(shortBits);
            case 4:
                int intBits = orderedView(littleEndian(1, args)).getInt(offset + pos);
                if (signed) {
                    return intBits;
                }
                return Conversions.intBitsToUint(intBits);
            default:
                throw new AssertionError();
        }
    }

    private static Object js_getFloat32(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        return realThis.js_getFloat(cx, s, 4, args);
    }

    private static Object js_getFloat64(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        return realThis.js_getFloat(cx, s, 8, args);
    }

    private static Object js_getFloat16(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        return realThis.js_getFloat(cx, s, 2, args);
    }

    private Object js_getFloat(Context cx, VarScope scope, int bytes, Object[] args) {
        int pos = ScriptRuntime.toIndex(isArg(args, 0) ? args[0] : Undefined.instance);

        if (isDataViewOutOfBounds()) {
            throw ScriptRuntime.typeErrorById("msg.dataview.bounds");
        }

        int viewSize = getByteLength();
        if ((long) pos + bytes > viewSize) {
            throw ScriptRuntime.rangeErrorById("msg.dataview.offset.range");
        }

        var view = orderedView(littleEndian(1, args));
        switch (bytes) {
            case 2:
                short shortBits = view.getShort(offset + pos);
                return Conversions.shortBitsToFloat16(shortBits);
            case 4:
                return view.getFloat(offset + pos);
            case 8:
                return view.getDouble(offset + pos);
            default:
                throw new AssertionError();
        }
    }

    private static Object js_setInt8(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        realThis.js_setInt(cx, s, 1, true, args);
        return Undefined.instance;
    }

    private static Object js_setInt16(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        realThis.js_setInt(cx, s, 2, true, args);
        return Undefined.instance;
    }

    private static Object js_setInt32(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        realThis.js_setInt(cx, s, 4, true, args);
        return Undefined.instance;
    }

    private static Object js_setUint8(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        realThis.js_setInt(cx, s, 1, false, args);
        return Undefined.instance;
    }

    private static Object js_setUint16(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        realThis.js_setInt(cx, s, 2, false, args);
        return Undefined.instance;
    }

    private static Object js_setUint32(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        realThis.js_setInt(cx, s, 4, false, args);
        return Undefined.instance;
    }

    private void js_setInt(Context cx, VarScope scope, int bytes, boolean signed, Object[] args) {
        int pos = ScriptRuntime.toIndex(isArg(args, 0) ? args[0] : Undefined.instance);

        Object val = isArg(args, 1) ? ScriptRuntime.toNumber(args[1]) : ScriptRuntime.zeroObj;

        if (isDataViewOutOfBounds()) {
            throw ScriptRuntime.typeErrorById("msg.dataview.bounds");
        }

        int viewSize = getByteLength();
        if ((long) pos + bytes > viewSize) {
            throw ScriptRuntime.rangeErrorById("msg.dataview.offset.range");
        }

        switch (bytes) {
            case 1:
                int byteBits = signed ? Conversions.toInt8(val) : Conversions.toUint8(val);
                if (pos + bytes > viewSize) {
                    throw ScriptRuntime.rangeErrorById("msg.dataview.offset.range");
                }
                arrayBuffer.buffer.put(offset + pos, (byte) byteBits);
                break;
            case 2:
                short shortBits = signed ? Conversions.toInt16(val) : Conversions.toUint16(val);
                if (pos + bytes > viewSize) {
                    throw ScriptRuntime.rangeErrorById("msg.dataview.offset.range");
                }
                orderedView(littleEndian(2, args)).putShort(offset + pos, shortBits);
                break;
            case 4:
                int intBits = signed ? Conversions.toInt32(val) : Conversions.toUint32(val);
                if (pos + bytes > viewSize) {
                    throw ScriptRuntime.rangeErrorById("msg.dataview.offset.range");
                }
                orderedView(littleEndian(2, args)).putInt(offset + pos, intBits);
                break;
            default:
                throw new AssertionError();
        }
    }

    private static Object js_setFloat32(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        realThis.js_setFloat(cx, s, 4, args);
        return Undefined.instance;
    }

    private static Object js_setFloat64(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        realThis.js_setFloat(cx, s, 8, args);
        return Undefined.instance;
    }

    private static Object js_setFloat16(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        realThis.js_setFloat(cx, s, 2, args);
        return Undefined.instance;
    }

    private void js_setFloat(Context cx, VarScope scope, int bytes, Object[] args) {
        int pos = ScriptRuntime.toIndex(isArg(args, 0) ? args[0] : Undefined.instance);

        double val = isArg(args, 1) ? ScriptRuntime.toNumber(args[1]) : Double.NaN;

        if (isDataViewOutOfBounds()) {
            throw ScriptRuntime.typeErrorById("msg.dataview.bounds");
        }

        int viewSize = getByteLength();
        if ((long) pos + bytes > viewSize) {
            throw ScriptRuntime.rangeErrorById("msg.dataview.offset.range");
        }

        var view = orderedView(littleEndian(2, args));
        switch (bytes) {
            case 2:
                short shortBits = Conversions.float16ToShortBits(val);
                view.putShort(offset + pos, shortBits);
                break;
            case 4:
                view.putFloat(offset + pos, (float) val);
                break;
            case 8:
                view.putDouble(offset + pos, val);
                break;
            default:
                throw new AssertionError();
        }
    }

    private static Object js_getBigInt64(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        return realThis.js_getBigInt(true, args);
    }

    private static Object js_getBigUint64(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        return realThis.js_getBigInt(false, args);
    }

    private Object js_getBigInt(boolean signed, Object[] args) {
        int pos = ScriptRuntime.toIndex(isArg(args, 0) ? args[0] : Undefined.instance);

        if (isDataViewOutOfBounds()) {
            throw ScriptRuntime.typeErrorById("msg.dataview.bounds");
        }

        int viewSize = getByteLength();
        if ((long) pos + 8 > viewSize) {
            throw ScriptRuntime.rangeErrorById("msg.dataview.offset.range");
        }

        long base = orderedView(littleEndian(1, args)).getLong(offset + pos);

        if (signed) {
            // Interpret as signed 64-bit integer
            return BigInteger.valueOf(base);
        } else {
            // Interpret as unsigned 64-bit integer
            return Conversions.longBitsToBigUint(base);
        }
    }

    // These two functions are specified separately, and sound like they should behave differently,
    // but end up having precisely the same effect, with only the interpretation of the 64 bits in
    // an intermediate state differing.
    private static Object js_setBigInt64(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        realThis.js_setBigInt(args);
        return Undefined.instance;
    }

    private static Object js_setBigUint64(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        NativeDataView realThis = realThis(thisObj);
        realThis.js_setBigInt(args);
        return Undefined.instance;
    }

    private void js_setBigInt(Object[] args) {
        int pos = ScriptRuntime.toIndex(isArg(args, 0) ? args[0] : Undefined.instance);

        BigInteger val = ScriptRuntime.toBigInt(isArg(args, 1) ? args[1] : Undefined.instance);

        if (isDataViewOutOfBounds()) {
            throw ScriptRuntime.typeErrorById("msg.dataview.bounds");
        }

        int viewSize = getByteLength();
        if ((long) pos + 8 > viewSize) {
            throw ScriptRuntime.rangeErrorById("msg.dataview.offset.range");
        }

        long base = val.longValue();
        orderedView(littleEndian(2, args)).putLong(offset + pos, base);
    }

    public boolean isDataViewOutOfBounds() {
        if (arrayBuffer.isDetached()) {
            return true;
        }

        int bufferByteLength = arrayBuffer.getLength();
        int byteOffsetEnd;
        if (autoLength) {
            byteOffsetEnd = bufferByteLength;
        } else {
            byteOffsetEnd = offset + byteLength;
        }

        return offset > bufferByteLength || byteOffsetEnd > bufferByteLength;
    }
}
