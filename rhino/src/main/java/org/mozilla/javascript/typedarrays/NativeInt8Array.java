/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
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

    public static Object init(Context cx, Scriptable scope, boolean sealed) {
        LambdaConstructor constructor =
                new LambdaConstructor(
                        scope,
                        CLASS_NAME,
                        3,
                        LambdaConstructor.CONSTRUCTOR_NEW,
                        (Context lcx, Scriptable lscope, Object[] args) ->
                                NativeTypedArrayView.js_constructor(
                                        lcx, lscope, args, NativeInt8Array::new, 1));
        constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);
        NativeTypedArrayView.init(cx, scope, constructor, NativeInt8Array::realThis);
        constructor.defineProperty("BYTES_PER_ELEMENT", 1, DONTENUM | READONLY | PERMANENT);
        constructor.definePrototypeProperty(
                "BYTES_PER_ELEMENT", 1, DONTENUM | READONLY | PERMANENT);

        ScriptRuntimeES6.addSymbolSpecies(cx, scope, constructor);
        if (sealed) {
            constructor.sealObject();
        }
        return constructor;
    }

    @Override
    public int getBytesPerElement() {
        return 1;
    }

    private static NativeInt8Array realThis(Scriptable thisObj) {
        return LambdaConstructor.convertThisObject(thisObj, NativeInt8Array.class);
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
