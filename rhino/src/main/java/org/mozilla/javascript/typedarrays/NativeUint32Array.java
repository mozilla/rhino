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
 * An array view that stores 32-bit quantities and implements the JavaScript "Uint32Array"
 * interface. It also implements List&lt;Long&gt; for direct manipulation in Java.
 */
public class NativeUint32Array extends NativeTypedArrayView<Long> {
    private static final long serialVersionUID = -7987831421954144244L;

    private static final String CLASS_NAME = "Uint32Array";
    private static final int BYTES_PER_ELEMENT = 4;

    public NativeUint32Array() {}

    public NativeUint32Array(NativeArrayBuffer ab, int off, int len) {
        super(ab, off, len, len * BYTES_PER_ELEMENT);
    }

    public NativeUint32Array(int len) {
        this(new NativeArrayBuffer((double) len * BYTES_PER_ELEMENT), 0, len);
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
                                        lcx,
                                        lscope,
                                        args,
                                        NativeUint32Array::new,
                                        BYTES_PER_ELEMENT));
        constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);
        NativeTypedArrayView.init(cx, scope, constructor, NativeUint32Array::realThis);
        constructor.defineProperty(
                "BYTES_PER_ELEMENT", BYTES_PER_ELEMENT, DONTENUM | READONLY | PERMANENT);
        constructor.definePrototypeProperty(
                "BYTES_PER_ELEMENT", BYTES_PER_ELEMENT, DONTENUM | READONLY | PERMANENT);

        ScriptRuntimeES6.addSymbolSpecies(cx, scope, constructor);
        if (sealed) {
            constructor.sealObject();
        }
        return constructor;
    }

    @Override
    public int getBytesPerElement() {
        return BYTES_PER_ELEMENT;
    }

    private static NativeUint32Array realThis(Scriptable thisObj) {
        return LambdaConstructor.convertThisObject(thisObj, NativeUint32Array.class);
    }

    @Override
    protected Object js_get(int index) {
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        return ByteIo.readUint32(
                arrayBuffer.buffer, (index * BYTES_PER_ELEMENT) + offset, useLittleEndian());
    }

    @Override
    protected Object js_set(int index, Object c) {
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        long val = Conversions.toUint32(c);
        ByteIo.writeUint32(
                arrayBuffer.buffer, (index * BYTES_PER_ELEMENT) + offset, val, useLittleEndian());
        return null;
    }

    @Override
    public Long get(int i) {
        if (checkIndex(i)) {
            throw new IndexOutOfBoundsException();
        }
        return (Long) js_get(i);
    }

    @Override
    public Long set(int i, Long aByte) {
        if (checkIndex(i)) {
            throw new IndexOutOfBoundsException();
        }
        return (Long) js_set(i, aByte);
    }
}
