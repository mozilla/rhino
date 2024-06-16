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
 * An array view that stores 16-bit quantities and implements the JavaScript "Int16Array" interface.
 * It also implements List&lt;Short&gt; for direct manipulation in Java.
 */
public class NativeInt16Array extends NativeTypedArrayView<Short> {
    private static final long serialVersionUID = -8592870435287581398L;

    private static final String CLASS_NAME = "Int16Array";
    private static final int BYTES_PER_ELEMENT = 2;

    public NativeInt16Array() {}

    public NativeInt16Array(NativeArrayBuffer ab, int off, int len) {
        super(ab, off, len, len * BYTES_PER_ELEMENT);
    }

    public NativeInt16Array(int len) {
        this(new NativeArrayBuffer((double) len * BYTES_PER_ELEMENT), 0, len);
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    public static void init(Context cx, Scriptable scope, boolean sealed) {
        NativeInt16Array a = new NativeInt16Array();
        IdFunctionObject constructor = a.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
        ScriptRuntimeES6.addSymbolSpecies(cx, scope, constructor);
    }

    @Override
    protected NativeInt16Array construct(NativeArrayBuffer ab, int off, int len) {
        return new NativeInt16Array(ab, off, len);
    }

    @Override
    public int getBytesPerElement() {
        return BYTES_PER_ELEMENT;
    }

    @Override
    protected NativeInt16Array realThis(Scriptable thisObj, IdFunctionObject f) {
        return ensureType(thisObj, NativeInt16Array.class, f);
    }

    @Override
    protected Object js_get(int index) {
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        return ByteIo.readInt16(
                arrayBuffer.buffer, (index * BYTES_PER_ELEMENT) + offset, useLittleEndian());
    }

    @Override
    protected Object js_set(int index, Object c) {
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        int val = Conversions.toInt16(c);
        ByteIo.writeInt16(
                arrayBuffer.buffer, (index * BYTES_PER_ELEMENT) + offset, val, useLittleEndian());
        return null;
    }

    @Override
    public Short get(int i) {
        if (checkIndex(i)) {
            throw new IndexOutOfBoundsException();
        }
        return (Short) js_get(i);
    }

    @Override
    public Short set(int i, Short aByte) {
        if (checkIndex(i)) {
            throw new IndexOutOfBoundsException();
        }
        return (Short) js_set(i, aByte);
    }
}
