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
 * An array view that stores 64-bit quantities and implements the JavaScript "Float64Array"
 * interface. It also implements List&lt;Double&gt; for direct manipulation in Java.
 */
public class NativeFloat64Array extends NativeTypedArrayView<Double> {
    private static final long serialVersionUID = -1255405650050639335L;

    private static final String CLASS_NAME = "Float64Array";
    private static final int BYTES_PER_ELEMENT = 8;

    public NativeFloat64Array() {}

    public NativeFloat64Array(NativeArrayBuffer ab, int off, int len) {
        super(ab, off, len, len * BYTES_PER_ELEMENT);
    }

    public NativeFloat64Array(int len) {
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
                                        NativeFloat64Array::new,
                                        BYTES_PER_ELEMENT));
        constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);
        NativeTypedArrayView.init(cx, scope, constructor, NativeFloat64Array::realThis);
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

    private static NativeFloat64Array realThis(Scriptable thisObj) {
        return LambdaConstructor.convertThisObject(thisObj, NativeFloat64Array.class);
    }

    @Override
    protected Object js_get(int index) {
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        long base =
                ByteIo.readUint64Primitive(
                        arrayBuffer.buffer,
                        (index * BYTES_PER_ELEMENT) + offset,
                        useLittleEndian());
        return Double.valueOf(Double.longBitsToDouble(base));
    }

    @Override
    protected Object js_set(int index, Object c) {
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        double val = ScriptRuntime.toNumber(c);
        long base = Double.doubleToLongBits(val);
        ByteIo.writeUint64(
                arrayBuffer.buffer, (index * BYTES_PER_ELEMENT) + offset, base, useLittleEndian());
        return null;
    }

    @Override
    public Double get(int i) {
        if (checkIndex(i)) {
            throw new IndexOutOfBoundsException();
        }
        return (Double) js_get(i);
    }

    @Override
    public Double set(int i, Double aByte) {
        if (checkIndex(i)) {
            throw new IndexOutOfBoundsException();
        }
        return (Double) js_set(i, aByte);
    }
}
