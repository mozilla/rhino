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
 * This class represents the JavaScript "DataView" interface, which allows direct manipulations of the
 * bytes in a NativeArrayBuffer. Java programmers would be best off getting the underling "byte[]" array
 * from the NativeArrayBuffer and manipulating it directly, perhaps using the "ByteIo" class as a helper.
 */

public class NativeDataView
    extends NativeArrayBufferView
{
    private static final long serialVersionUID = 1427967607557438968L;

    public static final String CLASS_NAME = "DataView";

    public NativeDataView()
    {
        super();
    }

    public NativeDataView(NativeArrayBuffer ab, int offset, int length)
    {
        super(ab, offset, length);
    }

    @Override
    public String getClassName()
    {
        return CLASS_NAME;
    }

    public static void init(Context cx, Scriptable scope, boolean sealed)
    {
        NativeDataView dv = new NativeDataView();
        dv.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    private void rangeCheck(int offset, int len)
    {
        if ((offset < 0) || ((offset + len) > byteLength)) {
            throw ScriptRuntime.constructError("RangeError", "offset out of range");
        }
    }

    private void checkOffset(Object[] args, int pos)
    {
        if (args.length <= pos) {
            throw ScriptRuntime.constructError("TypeError", "missing required offset parameter");
        }
        if (Undefined.instance.equals(args[pos])) {
            throw ScriptRuntime.constructError("RangeError", "invalid offset");
        }
    }

    private void checkValue(Object[] args, int pos)
    {
        if (args.length <= pos) {
            throw ScriptRuntime.constructError("TypeError", "missing required value parameter");
        }
        if (Undefined.instance.equals(args[pos])) {
            throw ScriptRuntime.constructError("RangeError", "invalid value parameter");
        }
    }

    private static NativeDataView realThis(Scriptable thisObj, IdFunctionObject f)
    {
        if (!(thisObj instanceof NativeDataView))
            throw incompatibleCallError(f);
        return (NativeDataView)thisObj;
    }

    private NativeDataView js_constructor(NativeArrayBuffer ab, int offset, int length)
    {
        if (length < 0) {
            throw ScriptRuntime.constructError("RangeError", "length out of range");
        }
        if ((offset < 0) || ((offset + length) > ab.getLength())) {
            throw ScriptRuntime.constructError("RangeError", "offset out of range");
        }
        return new NativeDataView(ab, offset, length);
    }

    private Object js_getInt(int bytes, boolean signed, Object[] args)
    {
        checkOffset(args, 0);

        int offset = ScriptRuntime.toInt32(args[0]);
        rangeCheck(offset, bytes);

        boolean littleEndian =
            (isArg(args, 1) && (bytes > 1) && ScriptRuntime.toBoolean(args[1]));

        switch (bytes) {
        case 1:
            return (signed ? ByteIo.readInt8(arrayBuffer.buffer, offset) :
                             ByteIo.readUint8(arrayBuffer.buffer, offset));
        case 2:
            return (signed ? ByteIo.readInt16(arrayBuffer.buffer, offset, littleEndian) :
                             ByteIo.readUint16(arrayBuffer.buffer, offset, littleEndian));
        case 4:
            return (signed ? ByteIo.readInt32(arrayBuffer.buffer, offset, littleEndian) :
                             ByteIo.readUint32(arrayBuffer.buffer, offset, littleEndian));
        default:
            throw new AssertionError();
        }
    }

    private Object js_getFloat(int bytes, Object[] args)
    {
        checkOffset(args, 0);

        int offset = ScriptRuntime.toInt32(args[0]);
        rangeCheck(offset, bytes);

        boolean littleEndian =
            (isArg(args, 1) && (bytes > 1) && ScriptRuntime.toBoolean(args[1]));

        switch (bytes) {
        case 4:
            return ByteIo.readFloat32(arrayBuffer.buffer, offset, littleEndian);
        case 8:
            return ByteIo.readFloat64(arrayBuffer.buffer, offset, littleEndian);
        default:
            throw new AssertionError();
        }
    }

    private void js_setInt(int bytes, boolean signed, Object[] args)
    {
        checkOffset(args, 0);
        checkValue(args, 1);

        int offset = ScriptRuntime.toInt32(args[0]);
        rangeCheck(offset, bytes);

        boolean littleEndian =
            (isArg(args, 2) && (bytes > 1) && ScriptRuntime.toBoolean(args[2]));

        switch (bytes) {
        case 1:
            if (signed) {
                ByteIo.writeInt8(arrayBuffer.buffer, offset, Conversions.toInt8(args[1]));
            } else {
                ByteIo.writeUint8(arrayBuffer.buffer, offset, Conversions.toUint8(args[1]));
            }
            break;
        case 2:
            if (signed) {
                ByteIo.writeInt16(arrayBuffer.buffer, offset, Conversions.toInt16(args[1]), littleEndian);
            } else {
                ByteIo.writeUint16(arrayBuffer.buffer, offset, Conversions.toUint16(args[1]), littleEndian);
            }
            break;
        case 4:
            if (signed) {
                ByteIo.writeInt32(arrayBuffer.buffer, offset, Conversions.toInt32(args[1]), littleEndian);
            } else {
                ByteIo.writeUint32(arrayBuffer.buffer, offset, Conversions.toUint32(args[1]), littleEndian);
            }
            break;
        default:
            throw new AssertionError();
        }
    }

    private void js_setFloat(int bytes, Object[] args)
    {
        checkOffset(args, 0);
        checkValue(args, 1);

        int offset = ScriptRuntime.toInt32(args[0]);
        rangeCheck(offset, bytes);

        boolean littleEndian =
            (isArg(args, 2) && (bytes > 1) && ScriptRuntime.toBoolean(args[2]));
        double val = ScriptRuntime.toNumber(args[1]);

        switch (bytes) {
        case 4:
            ByteIo.writeFloat32(arrayBuffer.buffer, offset, val, littleEndian);
            break;
        case 8:
            ByteIo.writeFloat64(arrayBuffer.buffer, offset, val, littleEndian);
            break;
        default:
            throw new AssertionError();
        }
    }

    // Function dispatcher

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Scriptable thisObj, Object[] args)
    {
        if (!f.hasTag(getClassName())) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        switch (id) {
        case Id_constructor:
            if (isArg(args, 0) && (args[0] instanceof NativeArrayBuffer)) {
                NativeArrayBuffer ab = (NativeArrayBuffer)args[0];
                int off = isArg(args, 1) ? ScriptRuntime.toInt32(args[1]) : 0;
                int len = isArg(args, 2) ? ScriptRuntime.toInt32(args[2]) : ab.getLength() - off;
                return js_constructor(ab, off, len);
            } else {
                throw ScriptRuntime.constructError("TypeError", "Missing parameters");
            }
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
    protected void initPrototypeId(int id)
    {
        String s;
        int arity;
        switch (id) {
        case Id_constructor:    arity = 1; s = "constructor"; break;
        case Id_getInt8:        arity = 1; s = "getInt8"; break;
        case Id_getUint8:       arity = 1; s = "getUint8"; break;
        case Id_getInt16:       arity = 1; s = "getInt16"; break;
        case Id_getUint16:      arity = 1; s = "getUint16"; break;
        case Id_getInt32:       arity = 1; s = "getInt32"; break;
        case Id_getUint32:      arity = 1; s = "getUint32"; break;
        case Id_getFloat32:     arity = 1; s = "getFloat32"; break;
        case Id_getFloat64:     arity = 1; s = "getFloat64"; break;
        case Id_setInt8:        arity = 2; s = "setInt8"; break;
        case Id_setUint8:       arity = 2; s = "setUint8"; break;
        case Id_setInt16:       arity = 2; s = "setInt16"; break;
        case Id_setUint16:      arity = 2; s = "setUint16"; break;
        case Id_setInt32:       arity = 2; s = "setInt32"; break;
        case Id_setUint32:      arity = 2; s = "setUint32"; break;
        case Id_setFloat32:     arity = 2; s = "setFloat32"; break;
        case Id_setFloat64:     arity = 2; s = "setFloat64"; break;
        default: throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(getClassName(), id, s, arity);
    }

// #string_id_map#

    @Override
    protected int findPrototypeId(String s)
    {
        int id;
// #generated# Last update: 2014-12-08 17:26:24 PST
        L0: { id = 0; String X = null; int c;
            L: switch (s.length()) {
            case 7: c=s.charAt(0);
                if (c=='g') { X="getInt8";id=Id_getInt8; }
                else if (c=='s') { X="setInt8";id=Id_setInt8; }
                break L;
            case 8: c=s.charAt(6);
                if (c=='1') {
                    c=s.charAt(0);
                    if (c=='g') { X="getInt16";id=Id_getInt16; }
                    else if (c=='s') { X="setInt16";id=Id_setInt16; }
                }
                else if (c=='3') {
                    c=s.charAt(0);
                    if (c=='g') { X="getInt32";id=Id_getInt32; }
                    else if (c=='s') { X="setInt32";id=Id_setInt32; }
                }
                else if (c=='t') {
                    c=s.charAt(0);
                    if (c=='g') { X="getUint8";id=Id_getUint8; }
                    else if (c=='s') { X="setUint8";id=Id_setUint8; }
                }
                break L;
            case 9: c=s.charAt(0);
                if (c=='g') {
                    c=s.charAt(8);
                    if (c=='2') { X="getUint32";id=Id_getUint32; }
                    else if (c=='6') { X="getUint16";id=Id_getUint16; }
                }
                else if (c=='s') {
                    c=s.charAt(8);
                    if (c=='2') { X="setUint32";id=Id_setUint32; }
                    else if (c=='6') { X="setUint16";id=Id_setUint16; }
                }
                break L;
            case 10: c=s.charAt(0);
                if (c=='g') {
                    c=s.charAt(9);
                    if (c=='2') { X="getFloat32";id=Id_getFloat32; }
                    else if (c=='4') { X="getFloat64";id=Id_getFloat64; }
                }
                else if (c=='s') {
                    c=s.charAt(9);
                    if (c=='2') { X="setFloat32";id=Id_setFloat32; }
                    else if (c=='4') { X="setFloat64";id=Id_setFloat64; }
                }
                break L;
            case 11: X="constructor";id=Id_constructor; break L;
            }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
        return id;
    }

    private static final int
        Id_constructor     = 1,
        Id_getInt8         = 2,
        Id_getUint8        = 3,
        Id_getInt16        = 4,
        Id_getUint16       = 5,
        Id_getInt32        = 6,
        Id_getUint32       = 7,
        Id_getFloat32      = 8,
        Id_getFloat64      = 9,
        Id_setInt8         = 10,
        Id_setUint8        = 11,
        Id_setInt16        = 12,
        Id_setUint16       = 13,
        Id_setInt32        = 14,
        Id_setUint32       = 15,
        Id_setFloat32      = 16,
        Id_setFloat64      = 17,
        MAX_PROTOTYPE_ID   = Id_setFloat64;

// #/string_id_map#
}
