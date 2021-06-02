/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionObject;
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

    public static void init(Context cx, Scriptable scope, boolean sealed) {
        NativeDataView dv = new NativeDataView();
        dv.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
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

    private static NativeDataView realThis(Scriptable thisObj, IdFunctionObject f) {
        return ensureType(thisObj, NativeDataView.class, f);
    }

    private static NativeDataView js_constructor(Object[] args) {
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

    // Function dispatcher

    @Override
    public Object execIdCall(
            IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(getClassName())) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        switch (id) {
            case Id_constructor:
                return js_constructor(args);
            case Id_getInt8:
                return realThis(thisObj, f).js_getInt(1, true, args);
            case Id_getUint8:
                return realThis(thisObj, f).js_getInt(1, false, args);
            case Id_getInt16:
                return realThis(thisObj, f).js_getInt(2, true, args);
            case Id_getUint16:
                return realThis(thisObj, f).js_getInt(2, false, args);
            case Id_getInt32:
                return realThis(thisObj, f).js_getInt(4, true, args);
            case Id_getUint32:
                return realThis(thisObj, f).js_getInt(4, false, args);
            case Id_getFloat32:
                return realThis(thisObj, f).js_getFloat(4, args);
            case Id_getFloat64:
                return realThis(thisObj, f).js_getFloat(8, args);
            case Id_setInt8:
                realThis(thisObj, f).js_setInt(1, true, args);
                return Undefined.instance;
            case Id_setUint8:
                realThis(thisObj, f).js_setInt(1, false, args);
                return Undefined.instance;
            case Id_setInt16:
                realThis(thisObj, f).js_setInt(2, true, args);
                return Undefined.instance;
            case Id_setUint16:
                realThis(thisObj, f).js_setInt(2, false, args);
                return Undefined.instance;
            case Id_setInt32:
                realThis(thisObj, f).js_setInt(4, true, args);
                return Undefined.instance;
            case Id_setUint32:
                realThis(thisObj, f).js_setInt(4, false, args);
                return Undefined.instance;
            case Id_setFloat32:
                realThis(thisObj, f).js_setFloat(4, args);
                return Undefined.instance;
            case Id_setFloat64:
                realThis(thisObj, f).js_setFloat(8, args);
                return Undefined.instance;
        }
        throw new IllegalArgumentException(String.valueOf(id));
    }

    @Override
    protected void initPrototypeId(int id) {
        String s;
        int arity;
        switch (id) {
            case Id_constructor:
                arity = 1;
                s = "constructor";
                break;
            case Id_getInt8:
                arity = 1;
                s = "getInt8";
                break;
            case Id_getUint8:
                arity = 1;
                s = "getUint8";
                break;
            case Id_getInt16:
                arity = 1;
                s = "getInt16";
                break;
            case Id_getUint16:
                arity = 1;
                s = "getUint16";
                break;
            case Id_getInt32:
                arity = 1;
                s = "getInt32";
                break;
            case Id_getUint32:
                arity = 1;
                s = "getUint32";
                break;
            case Id_getFloat32:
                arity = 1;
                s = "getFloat32";
                break;
            case Id_getFloat64:
                arity = 1;
                s = "getFloat64";
                break;
            case Id_setInt8:
                arity = 2;
                s = "setInt8";
                break;
            case Id_setUint8:
                arity = 2;
                s = "setUint8";
                break;
            case Id_setInt16:
                arity = 2;
                s = "setInt16";
                break;
            case Id_setUint16:
                arity = 2;
                s = "setUint16";
                break;
            case Id_setInt32:
                arity = 2;
                s = "setInt32";
                break;
            case Id_setUint32:
                arity = 2;
                s = "setUint32";
                break;
            case Id_setFloat32:
                arity = 2;
                s = "setFloat32";
                break;
            case Id_setFloat64:
                arity = 2;
                s = "setFloat64";
                break;
            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(getClassName(), id, s, arity);
    }

    @Override
    protected int findPrototypeId(String s) {
        int id;
        switch (s) {
            case "constructor":
                id = Id_constructor;
                break;
            case "getInt8":
                id = Id_getInt8;
                break;
            case "getUint8":
                id = Id_getUint8;
                break;
            case "getInt16":
                id = Id_getInt16;
                break;
            case "getUint16":
                id = Id_getUint16;
                break;
            case "getInt32":
                id = Id_getInt32;
                break;
            case "getUint32":
                id = Id_getUint32;
                break;
            case "getFloat32":
                id = Id_getFloat32;
                break;
            case "getFloat64":
                id = Id_getFloat64;
                break;
            case "setInt8":
                id = Id_setInt8;
                break;
            case "setUint8":
                id = Id_setUint8;
                break;
            case "setInt16":
                id = Id_setInt16;
                break;
            case "setUint16":
                id = Id_setUint16;
                break;
            case "setInt32":
                id = Id_setInt32;
                break;
            case "setUint32":
                id = Id_setUint32;
                break;
            case "setFloat32":
                id = Id_setFloat32;
                break;
            case "setFloat64":
                id = Id_setFloat64;
                break;
            default:
                id = 0;
                break;
        }
        return id;
    }

    private static final int Id_constructor = 1,
            Id_getInt8 = 2,
            Id_getUint8 = 3,
            Id_getInt16 = 4,
            Id_getUint16 = 5,
            Id_getInt32 = 6,
            Id_getUint32 = 7,
            Id_getFloat32 = 8,
            Id_getFloat64 = 9,
            Id_setInt8 = 10,
            Id_setUint8 = 11,
            Id_setInt16 = 12,
            Id_setUint16 = 13,
            Id_setInt32 = 14,
            Id_setUint32 = 15,
            Id_setFloat32 = 16,
            Id_setFloat64 = 17,
            MAX_PROTOTYPE_ID = Id_setFloat64;
}
