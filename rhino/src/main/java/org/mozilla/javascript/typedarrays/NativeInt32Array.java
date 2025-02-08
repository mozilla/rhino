/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.ScriptRuntimeES6;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

/**
 * An array view that stores 32-bit quantities and implements the JavaScript "Int32Array" interface.
 * It also implements List&lt;Integer&gt; for direct manipulation in Java.
 */
public class NativeInt32Array extends NativeTypedArrayView<Integer> {
    private static final long serialVersionUID = -8963461831950499340L;

    private static final String CLASS_NAME = "Int32Array";
    private static final int BYTES_PER_ELEMENT = 4;

    public NativeInt32Array() {}

    public NativeInt32Array(NativeArrayBuffer ab, int off, int len) {
        super(ab, off, len, len * BYTES_PER_ELEMENT);
    }

    public NativeInt32Array(int len) {
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
                                        NativeInt32Array::new,
                                        BYTES_PER_ELEMENT));
        constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);
        NativeTypedArrayView.init(cx, scope, constructor, NativeInt32Array::realThis);
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

    private static NativeInt32Array realThis(Scriptable thisObj) {
        return LambdaConstructor.convertThisObject(thisObj, NativeInt32Array.class);
    }

    @Override
    protected Object js_get(int index) {
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        return ByteIo.readInt32(
                arrayBuffer.buffer, (index * BYTES_PER_ELEMENT) + offset, useLittleEndian());
    }

    @Override
    protected Object js_set(int index, Object c) {
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        int val = ScriptRuntime.toInt32(c);
        ByteIo.writeInt32(
                arrayBuffer.buffer, (index * BYTES_PER_ELEMENT) + offset, val, useLittleEndian());
        return null;
    }

    @Override
    public Integer get(int i) {
        if (checkIndex(i)) {
            throw new IndexOutOfBoundsException();
        }
        return (Integer) js_get(i);
    }

    @Override
    public Integer set(int i, Integer aByte) {
        if (checkIndex(i)) {
            throw new IndexOutOfBoundsException();
        }
        return (Integer) js_set(i, aByte);
    }
}
