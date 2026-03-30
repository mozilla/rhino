/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import static org.mozilla.javascript.ClassDescriptor.Builder.value;
import static org.mozilla.javascript.ClassDescriptor.Destination.CTOR;
import static org.mozilla.javascript.ClassDescriptor.Destination.PROTO;

import org.mozilla.javascript.ClassDescriptor;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JSFunction;
import org.mozilla.javascript.ScriptRuntimeES6;
import org.mozilla.javascript.SymbolKey;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.VarScope;

/**
 * An array view that stores 8-bit quantities and implements the JavaScript "Int8Array" interface.
 * It also implements List&lt;Byte&gt; for direct manipulation in Java.
 */
public class NativeInt8Array extends NativeTypedArrayView<Byte> {
    private static final long serialVersionUID = -3349419704390398895L;

    private static final String CLASS_NAME = "Int8Array";

    private static final ClassDescriptor DESCRIPTOR;

    static {
        DESCRIPTOR =
                new ClassDescriptor.Builder(
                                CLASS_NAME,
                                3,
                                NativeTypedArrayView::typeError,
                                NativeInt8Array::js_constructor)
                        .withProp(CTOR, "BYTES_PER_ELEMENT", value(1))
                        .withProp(PROTO, "BYTES_PER_ELEMENT", value(1))
                        .withProp(CTOR, SymbolKey.SPECIES, ScriptRuntimeES6::symbolSpecies)
                        .build();
    }

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

    public static JSFunction init(Context cx, VarScope scope, boolean sealed) {
        return NativeTypedArrayView.initSubClass(cx, scope, DESCRIPTOR, sealed);
    }

    @Override
    public int getBytesPerElement() {
        return 1;
    }

    private static Object js_constructor(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return NativeTypedArrayView.js_constructor(
                cx, f, nt, s, thisObj, args, NativeInt8Array::new, 1);
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
        int val = Conversions.toInt8(c);
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        ByteIo.writeInt8(arrayBuffer.buffer, index + offset, val);
        return null;
    }

    // List implementation (much of it handled by the superclass)

    @Override
    public Byte get(int i) {
        ensureIndex(i);
        return (Byte) js_get(i);
    }

    @Override
    public Byte set(int i, Byte aByte) {
        ensureIndex(i);
        return (Byte) js_set(i, aByte);
    }
}
