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
 * An array view that stores 16-bit quantities and implements the JavaScript "Uint16Array"
 * interface. It also implements List&lt;Integer&gt; for direct manipulation in Java.
 */
public class NativeUint16Array extends NativeTypedArrayView<Integer> {
    private static final long serialVersionUID = 7700018949434240321L;

    private static final String CLASS_NAME = "Uint16Array";
    private static final int BYTES_PER_ELEMENT = 2;

    private static final ClassDescriptor DESCRIPTOR;

    static {
        DESCRIPTOR =
                new ClassDescriptor.Builder(
                                CLASS_NAME,
                                3,
                                NativeTypedArrayView::typeError,
                                NativeUint16Array::js_constructor)
                        .withProp(CTOR, "BYTES_PER_ELEMENT", value(2))
                        .withProp(PROTO, "BYTES_PER_ELEMENT", value(2))
                        .withProp(CTOR, SymbolKey.SPECIES, ScriptRuntimeES6::symbolSpecies)
                        .build();
    }

    public NativeUint16Array() {}

    public NativeUint16Array(NativeArrayBuffer ab, int off, int len) {
        super(ab, off, len, len * BYTES_PER_ELEMENT);
    }

    public NativeUint16Array(int len) {
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
                cx, f, nt, s, thisObj, args, NativeUint16Array::new, 2);
    }

    @Override
    protected Object js_get(int index) {
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        return ByteIo.readUint16(
                arrayBuffer.buffer, (index * BYTES_PER_ELEMENT) + offset, useLittleEndian());
    }

    @Override
    protected Object js_set(int index, Object c) {
        int val = Conversions.toUint16(c);
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        ByteIo.writeUint16(
                arrayBuffer.buffer, (index * BYTES_PER_ELEMENT) + offset, val, useLittleEndian());
        return null;
    }

    @Override
    public Integer get(int i) {
        ensureIndex(i);
        return (Integer) js_get(i);
    }

    @Override
    public Integer set(int i, Integer aByte) {
        ensureIndex(i);
        return (Integer) js_set(i, aByte);
    }
}
