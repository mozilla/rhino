/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.ScriptRuntimeES6;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

/**
 * An array view that stores 8-bit quantities and implements the JavaScript "Int8Array" interface.
 * It also implements List&lt;Byte&gt; for direct manipulation in Java.
 */
public class NativeInt8Array extends NativeTypedArrayView<Byte> {
    private static final long serialVersionUID = -3349419704390398895L;

    private static final String CLASS_NAME = "Int8Array";

    public NativeInt8Array() {}

    public NativeInt8Array(NativeArrayBuffer ab, int off, int len) {
        super(ab, off, len, len);
    }

    public NativeInt8Array(int len) {
        this(new NativeArrayBuffer(len), 0, len);
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    public static void init(Context cx, Scriptable scope, boolean sealed) {
        NativeInt8Array a = new NativeInt8Array();
        IdFunctionObject constructor = a.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
        ScriptRuntimeES6.addSymbolSpecies(cx, scope, constructor);
    }

    @Override
    protected NativeInt8Array construct(NativeArrayBuffer ab, int off, int len) {
        return new NativeInt8Array(ab, off, len);
    }

    @Override
    public int getBytesPerElement() {
        return 1;
    }

    @Override
    protected NativeInt8Array realThis(Scriptable thisObj, IdFunctionObject f) {
        return ensureType(thisObj, NativeInt8Array.class, f);
    }

    @Override
    protected Object js_get(int index) {
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        return ByteIo.readInt8(arrayBuffer.buffer, index + offset);
    }

    @Override
    protected Object js_set(int index, Object c) {
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        int val = Conversions.toInt8(c);
        ByteIo.writeInt8(arrayBuffer.buffer, index + offset, val);
        return null;
    }

    // List implementation (much of it handled by the superclass)

    @Override
    public Byte get(int i) {
        if (checkIndex(i)) {
            throw new IndexOutOfBoundsException();
        }
        return (Byte) js_get(i);
    }

    @Override
    public Byte set(int i, Byte aByte) {
        if (checkIndex(i)) {
            throw new IndexOutOfBoundsException();
        }
        return (Byte) js_set(i, aByte);
    }
}
