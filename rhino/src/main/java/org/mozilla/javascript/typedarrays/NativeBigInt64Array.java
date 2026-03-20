/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import static org.mozilla.javascript.ClassDescriptor.Builder.value;
import static org.mozilla.javascript.ClassDescriptor.Destination.CTOR;
import static org.mozilla.javascript.ClassDescriptor.Destination.PROTO;

import java.math.BigInteger;
import org.mozilla.javascript.ClassDescriptor;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JSFunction;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.ScriptRuntimeES6;
import org.mozilla.javascript.SymbolKey;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.VarScope;

/**
 * An array view that stores 64-bit quantities and implements the JavaScript "Float64Array"
 * interface. It also implements List&lt;Double&gt; for direct manipulation in Java.
 */
public class NativeBigInt64Array extends NativeBigIntArrayView {
    private static final long serialVersionUID = -1255405650050639335L;

    private static final String CLASS_NAME = "BigInt64Array";
    private static final int BYTES_PER_ELEMENT = 8;

    private static final ClassDescriptor DESCRIPTOR;

    static {
        DESCRIPTOR =
                new ClassDescriptor.Builder(
                                CLASS_NAME,
                                3,
                                NativeTypedArrayView::typeError,
                                NativeBigInt64Array::js_constructor)
                        .withProp(CTOR, "BYTES_PER_ELEMENT", value(8))
                        .withProp(PROTO, "BYTES_PER_ELEMENT", value(8))
                        .withProp(CTOR, SymbolKey.SPECIES, ScriptRuntimeES6::symbolSpecies)
                        .build();
    }

    public NativeBigInt64Array() {}

    public NativeBigInt64Array(NativeArrayBuffer ab, int off, int len) {
        super(ab, off, len, len * BYTES_PER_ELEMENT);
    }

    public NativeBigInt64Array(int len) {
        this(new NativeArrayBuffer((double) len * BYTES_PER_ELEMENT), 0, len);
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    public static JSFunction init(Context cx, VarScope scope, boolean sealed) {
        return NativeTypedArrayView.initSubClass(cx, scope, DESCRIPTOR, sealed);
    }

    @Override
    public int getBytesPerElement() {
        return BYTES_PER_ELEMENT;
    }

    private static Object js_constructor(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return NativeTypedArrayView.js_constructor(
                cx, f, nt, s, thisObj, args, NativeBigInt64Array::new, 8);
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
        return BigInteger.valueOf(base);
    }

    @Override
    protected Object js_set(int index, Object c) {
        var val = ScriptRuntime.toBigInt(c);
        if (checkIndex(index)) {
            return Undefined.instance;
        }

        long base = val.longValue();

        ByteIo.writeUint64(
                arrayBuffer.buffer, (index * BYTES_PER_ELEMENT) + offset, base, useLittleEndian());
        return null;
    }

    @Override
    public BigInteger get(int i) {
        if (checkIndex(i)) {
            throw new IndexOutOfBoundsException();
        }
        return (BigInteger) js_get(i);
    }

    @Override
    public BigInteger set(int i, BigInteger aByte) {
        if (checkIndex(i)) {
            throw new IndexOutOfBoundsException();
        }
        return (BigInteger) js_set(i, aByte);
    }
}
