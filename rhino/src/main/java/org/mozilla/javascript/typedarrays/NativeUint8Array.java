/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import static org.mozilla.javascript.ClassDescriptor.Builder.value;
import static org.mozilla.javascript.ClassDescriptor.Destination.CTOR;
import static org.mozilla.javascript.ClassDescriptor.Destination.PROTO;

import java.io.Serial;
import org.mozilla.javascript.ClassDescriptor;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JSFunction;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.ScriptRuntimeES6;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.SymbolKey;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.VarScope;

/**
 * An array view that stores 8-bit quantities and implements the JavaScript "Uint8Array" interface.
 * It also implements List&lt;Integer&gt; for direct manipulation in Java.
 */
public class NativeUint8Array extends NativeTypedArrayView<Integer> {
    @Serial private static final long serialVersionUID = 4309679036296927829L;

    private static final String CLASS_NAME = "Uint8Array";

    private static final ClassDescriptor DESCRIPTOR;

    static {
        DESCRIPTOR =
                new ClassDescriptor.Builder(
                                CLASS_NAME,
                                3,
                                NativeTypedArrayView::typeError,
                                NativeUint8Array::js_constructor)
                        .withProp(CTOR, "BYTES_PER_ELEMENT", value(1))
                        .withProp(PROTO, "BYTES_PER_ELEMENT", value(1))
                        .withProp(CTOR, SymbolKey.SPECIES, ScriptRuntimeES6::symbolSpecies)
                        .withMethod(CTOR, "fromBase64", 1, NativeUint8Array::js_fromBase64)
                        .withMethod(CTOR, "fromHex", 1, NativeUint8Array::js_fromHex)
                        .withMethod(PROTO, "setFromBase64", 1, NativeUint8Array::js_setFromBase64)
                        .withMethod(PROTO, "setFromHex", 1, NativeUint8Array::js_setFromHex)
                        .withMethod(PROTO, "toBase64", 0, NativeUint8Array::js_toBase64)
                        .withMethod(PROTO, "toHex", 0, NativeUint8Array::js_toHex)
                        .build();
    }

    public NativeUint8Array() {}

    public NativeUint8Array(NativeArrayBuffer ab, int off, int len) {
        super(ab, off, len, len);
    }

    public NativeUint8Array(int len) {
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

    private static NativeUint8Array realThis(Object thisObj) {
        return LambdaConstructor.convertThisObject(thisObj, NativeUint8Array.class);
    }

    private static NativeTypedArrayView<?> js_constructor(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        return NativeTypedArrayView.js_constructor(
                cx,
                f,
                nt,
                s,
                thisObj,
                args,
                NativeUint8Array::new,
                1,
                TopLevel.Builtins.Uint8Array);
    }

    private static Object js_fromBase64(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        var string = requireStringArg(args);
        var options = getOptionsObject(args, 1);
        var isBase64Url = getBase64AlphabetOption(options);
        var lastChunkHandlingString = getLastChunkHandlingOption(options);

        var result = Base64Codec.decode(string, isBase64Url, lastChunkHandlingString);
        return constructFromDecodeResult(cx, f, nt, s, thisObj, result);
    }

    private static Object js_fromHex(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        var string = requireStringArg(args);

        var result = HexCodec.decode(string);
        return constructFromDecodeResult(cx, f, nt, s, thisObj, result);
    }

    private static Object js_setFromBase64(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        var into = realThis(thisObj);
        var string = requireStringArg(args);
        var options = getOptionsObject(args, 1);
        var isBase64Url = getBase64AlphabetOption(options);
        var lastChunkHandlingString = getLastChunkHandlingOption(options);

        var length = into.validateAndGetLength();
        var result = Base64Codec.decode(string, isBase64Url, lastChunkHandlingString, length);
        return setFromDecodeResult(cx, s, into, result);
    }

    private static Object js_setFromHex(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        var into = realThis(thisObj);
        var string = requireStringArg(args);
        var length = into.validateAndGetLength();

        var result = HexCodec.decode(string, length);
        return setFromDecodeResult(cx, s, into, result);
    }

    private static Object js_toBase64(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        var self = realThis(thisObj);
        var options = getOptionsObject(args, 0);
        var isBase64Url = getBase64AlphabetOption(options);
        var omitPadding = getOmitPaddingOption(options);

        return Base64Codec.encode(copyBytes(self), isBase64Url, omitPadding);
    }

    private static Object js_toHex(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        var self = realThis(thisObj);
        return HexCodec.encode(copyBytes(self));
    }

    private static String requireStringArg(Object[] args) {
        if (!(isArg(args, 0) && args[0] instanceof CharSequence)) {
            throw ScriptRuntime.typeErrorById("msg.not.a.string");
        }
        return args[0].toString();
    }

    private static byte[] copyBytes(NativeTypedArrayView<?> self) {
        var length = self.validateAndGetLength();
        var bytes = new byte[(int) length];
        System.arraycopy(self.arrayBuffer.buffer, self.offset, bytes, 0, bytes.length);
        return bytes;
    }

    private static NativeTypedArrayView<?> constructFromDecodeResult(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, DecodeResult result) {
        if (result.error() != null) {
            throw result.error();
        }

        var resultLength = result.written();
        var ta = js_constructor(cx, f, nt, s, thisObj, new Object[] {resultLength});
        System.arraycopy(result.bytes(), 0, ta.arrayBuffer.buffer, ta.offset, resultLength);
        return ta;
    }

    private static Object setFromDecodeResult(
            Context cx, VarScope s, NativeTypedArrayView<?> into, DecodeResult result) {
        var written = result.written();
        System.arraycopy(result.bytes(), 0, into.arrayBuffer.buffer, into.offset, written);

        if (result.error() != null) {
            throw result.error();
        }

        var resultObj = cx.newObject(s);
        resultObj.put("read", resultObj, result.read());
        resultObj.put("written", resultObj, written);
        return resultObj;
    }

    private static NativeObject getOptionsObject(Object[] args, int index) {
        if (!isArg(args, index) || Undefined.isUndefined(args[index])) {
            return new NativeObject();
        }
        if (args[index] instanceof NativeObject obj) {
            return obj;
        }
        throw ScriptRuntime.typeErrorById("msg.not.an.object");
    }

    private static boolean getBase64AlphabetOption(NativeObject options) {
        var alphabet =
                getStringOption(
                        options,
                        "alphabet",
                        Base64Codec.BASE_64,
                        "msg.bad.alphabet",
                        new String[] {Base64Codec.BASE_64, Base64Codec.BASE_64_URL});
        return alphabet.equals(Base64Codec.BASE_64_URL);
    }

    private static String getLastChunkHandlingOption(NativeObject options) {
        return getStringOption(
                options,
                "lastChunkHandling",
                Base64Codec.LOOSE,
                "msg.bad.lastchunkhandling",
                new String[] {
                    Base64Codec.LOOSE, Base64Codec.STRICT, Base64Codec.STOP_BEFORE_PARTIAL
                });
    }

    private static boolean getOmitPaddingOption(NativeObject options) {
        var omitPaddingField = ScriptableObject.getProperty(options, "omitPadding");
        if (omitPaddingField == NOT_FOUND) {
            omitPaddingField = Undefined.instance;
        }
        return ScriptRuntime.toBoolean(omitPaddingField);
    }

    private static String getStringOption(
            NativeObject options,
            String name,
            String defaultValue,
            String errorMessageId,
            String[] allowedValues) {
        var value = ScriptableObject.getProperty(options, name);
        if (value == NOT_FOUND || Undefined.isUndefined(value)) {
            value = defaultValue;
        }
        var stringValue = value.toString();
        if (value instanceof CharSequence) {
            for (var allowed : allowedValues) {
                if (allowed.equals(stringValue)) {
                    return stringValue;
                }
            }
        }
        throw ScriptRuntime.typeErrorById(errorMessageId);
    }

    @Override
    protected Object js_get(int index) {
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        int byteBits = arrayBuffer.buffer.get(index + offset);
        return Conversions.byteBitsToUint(byteBits);
    }

    @Override
    protected Object js_set(int index, Object c) {
        int val = Conversions.toUint8(c);
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        arrayBuffer.buffer.put(index + offset, (byte) val);
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
