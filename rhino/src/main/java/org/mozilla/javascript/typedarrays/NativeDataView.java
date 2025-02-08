/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
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
                cx,
                "buffer",
                (Scriptable thisObj) -> realThis(thisObj).arrayBuffer,
                DONTENUM | READONLY);
        constructor.definePrototypeProperty(
                cx,
                "byteLength",
                (Scriptable thisObj) -> realThis(thisObj).byteLength,
                DONTENUM | READONLY);
        constructor.definePrototypeProperty(
                cx,
                "byteOffset",
                (Scriptable thisObj) -> realThis(thisObj).offset,
                DONTENUM | READONLY);

        constructor.definePrototypeMethod(
                scope,
                "getFloat32",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        realThis(thisObj).js_getFloat(4, args),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "getFloat64",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        realThis(thisObj).js_getFloat(8, args),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "getInt8",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        realThis(thisObj).js_getInt(1, true, args),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "getInt16",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        realThis(thisObj).js_getInt(2, true, args),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "getInt32",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        realThis(thisObj).js_getInt(4, true, args),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "getUint8",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        realThis(thisObj).js_getInt(1, false, args),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "getUint16",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        realThis(thisObj).js_getInt(2, false, args),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "getUint32",
                1,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) ->
                        realThis(thisObj).js_getInt(4, false, args),
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "setFloat32",
                2,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    realThis(thisObj).js_setFloat(4, args);
                    return Undefined.instance;
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "setFloat64",
                2,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    realThis(thisObj).js_setFloat(8, args);
                    return Undefined.instance;
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "setInt8",
                2,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    realThis(thisObj).js_setInt(1, true, args);
                    return Undefined.instance;
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "setInt16",
                2,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    realThis(thisObj).js_setInt(2, true, args);
                    return Undefined.instance;
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "setInt32",
                2,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    realThis(thisObj).js_setInt(4, true, args);
                    return Undefined.instance;
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "setUint8",
                2,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    realThis(thisObj).js_setInt(1, false, args);
                    return Undefined.instance;
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "setUint16",
                2,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    realThis(thisObj).js_setInt(2, false, args);
                    return Undefined.instance;
                },
                DONTENUM,
                DONTENUM | READONLY);
        constructor.definePrototypeMethod(
                scope,
                "setUint32",
                2,
                (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
                    realThis(thisObj).js_setInt(4, false, args);
                    return Undefined.instance;
                },
                DONTENUM,
                DONTENUM | READONLY);

        if (sealed) {
            constructor.sealObject();
        }
        return constructor;
    }

    private static int determinePos(Object[] args) {
        if (isArg(args, 0)) {
            double doublePos = ScriptRuntime.toNumber(args[0]);
            if (Double.isInfinite(doublePos)) {
                throw ScriptRuntime.rangeError("offset out of range");
            }
            return ScriptRuntime.toInt32(doublePos);
        }
        return 0;
    }

    private void rangeCheck(int pos, int len) {
        if ((pos < 0) || ((pos + len) > byteLength)) {
            throw ScriptRuntime.rangeError("offset out of range");
        }
    }

    private static NativeDataView realThis(Scriptable thisObj) {
        return LambdaConstructor.convertThisObject(thisObj, NativeDataView.class);
    }

    private static NativeDataView js_constructor(Context cx, Scriptable scope, Object[] args) {
        if (!isArg(args, 0) || !(args[0] instanceof NativeArrayBuffer)) {
            throw ScriptRuntime.constructError("TypeError", "Missing parameters");
        }

        NativeArrayBuffer ab = (NativeArrayBuffer) args[0];

        int pos;
        if (isArg(args, 1)) {
            double doublePos = ScriptRuntime.toNumber(args[1]);
            if (Double.isInfinite(doublePos)) {
                throw ScriptRuntime.rangeError("offset out of range");
            }
            pos = ScriptRuntime.toInt32(doublePos);
        } else {
            pos = 0;
        }

        int len;
        if (isArg(args, 2)) {
            double doublePos = ScriptRuntime.toNumber(args[2]);
            if (Double.isInfinite(doublePos)) {
                throw ScriptRuntime.rangeError("offset out of range");
            }
            len = ScriptRuntime.toInt32(doublePos);
        } else {
            len = ab.getLength() - pos;
        }

        if (len < 0) {
            throw ScriptRuntime.rangeError("length out of range");
        }
        if ((pos < 0) || ((pos + len) > ab.getLength())) {
            throw ScriptRuntime.rangeError("offset out of range");
        }
        return new NativeDataView(ab, pos, len);
    }

    private Object js_getInt(int bytes, boolean signed, Object[] args) {
        int pos = determinePos(args);
        rangeCheck(pos, bytes);

        boolean littleEndian = isArg(args, 1) && (bytes > 1) && ScriptRuntime.toBoolean(args[1]);

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

    private Object js_getFloat(int bytes, Object[] args) {
        int pos = determinePos(args);
        rangeCheck(pos, bytes);

        boolean littleEndian = isArg(args, 1) && (bytes > 1) && ScriptRuntime.toBoolean(args[1]);

        switch (bytes) {
            case 4:
                return ByteIo.readFloat32(arrayBuffer.buffer, offset + pos, littleEndian);
            case 8:
                return ByteIo.readFloat64(arrayBuffer.buffer, offset + pos, littleEndian);
            default:
                throw new AssertionError();
        }
    }

    private void js_setInt(int bytes, boolean signed, Object[] args) {
        int pos = determinePos(args);
        if (pos < 0) {
            throw ScriptRuntime.rangeError("offset out of range");
        }

        boolean littleEndian = isArg(args, 2) && (bytes > 1) && ScriptRuntime.toBoolean(args[2]);

        Object val = ScriptRuntime.zeroObj;
        if (args.length > 1) {
            val = args[1];
        }

        switch (bytes) {
            case 1:
                if (signed) {
                    int value = Conversions.toInt8(val);
                    if (pos + bytes > byteLength) {
                        throw ScriptRuntime.rangeError("offset out of range");
                    }
                    ByteIo.writeInt8(arrayBuffer.buffer, offset + pos, value);
                } else {
                    int value = Conversions.toUint8(val);
                    if (pos + bytes > byteLength) {
                        throw ScriptRuntime.rangeError("offset out of range");
                    }
                    ByteIo.writeUint8(arrayBuffer.buffer, offset + pos, value);
                }
                break;
            case 2:
                if (signed) {
                    int value = Conversions.toInt16(val);
                    if (pos + bytes > byteLength) {
                        throw ScriptRuntime.rangeError("offset out of range");
                    }
                    ByteIo.writeInt16(arrayBuffer.buffer, offset + pos, value, littleEndian);
                } else {
                    int value = Conversions.toUint16(val);
                    if (pos + bytes > byteLength) {
                        throw ScriptRuntime.rangeError("offset out of range");
                    }
                    ByteIo.writeUint16(arrayBuffer.buffer, offset + pos, value, littleEndian);
                }
                break;
            case 4:
                if (signed) {
                    int value = Conversions.toInt32(val);
                    if (pos + bytes > byteLength) {
                        throw ScriptRuntime.rangeError("offset out of range");
                    }
                    ByteIo.writeInt32(arrayBuffer.buffer, offset + pos, value, littleEndian);
                } else {
                    long value = Conversions.toUint32(val);
                    if (pos + bytes > byteLength) {
                        throw ScriptRuntime.rangeError("offset out of range");
                    }
                    ByteIo.writeUint32(arrayBuffer.buffer, offset + pos, value, littleEndian);
                }
                break;
            default:
                throw new AssertionError();
        }
    }

    private void js_setFloat(int bytes, Object[] args) {
        int pos = determinePos(args);
        if (pos < 0) {
            throw ScriptRuntime.rangeError("offset out of range");
        }

        boolean littleEndian = isArg(args, 2) && (bytes > 1) && ScriptRuntime.toBoolean(args[2]);

        double val = Double.NaN;
        if (args.length > 1) {
            val = ScriptRuntime.toNumber(args[1]);
        }

        if (pos + bytes > byteLength) {
            throw ScriptRuntime.rangeError("offset out of range");
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
}
