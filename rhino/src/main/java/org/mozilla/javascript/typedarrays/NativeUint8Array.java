/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import static org.mozilla.javascript.ClassDescriptor.Builder.value;
import static org.mozilla.javascript.ClassDescriptor.Destination.CTOR;
import static org.mozilla.javascript.ClassDescriptor.Destination.PROTO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.mozilla.javascript.ClassDescriptor;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.JSFunction;
import org.mozilla.javascript.LambdaConstructor;
import org.mozilla.javascript.NativeNumber;
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

    private static final String BASE_64 = "base64";
    private static final String BASE_64_URL = "base64url";

    private static final String LOOSE = "loose";
    private static final String STRICT = "strict";
    private static final String STOP_BEFORE_PARTIAL = "stop-before-partial";

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
        if (!(isArg(args, 0) && args[0] instanceof CharSequence)) {
            throw ScriptRuntime.typeErrorById("msg.not.a.string");
        }

        var string = args[0].toString();
        var options = getOptionsObject(args, 1);

        var alphabet = ScriptableObject.getProperty(options, "alphabet");
        if (alphabet == NOT_FOUND || Undefined.isUndefined(alphabet)) {
            alphabet = BASE_64;
        }
        var alphabetString = alphabet.toString();

        if (!(alphabet instanceof CharSequence)
                || (!alphabetString.equals(BASE_64) && !alphabetString.equals(BASE_64_URL))) {
            throw ScriptRuntime.typeErrorById("msg.bad.alphabet");
        }

        var lastChunkHandling = ScriptableObject.getProperty(options, "lastChunkHandling");
        if (lastChunkHandling == NOT_FOUND || Undefined.isUndefined(lastChunkHandling)) {
            lastChunkHandling = LOOSE;
        }
        var lastChunkHandlingString = lastChunkHandling.toString();

        if (!(lastChunkHandling instanceof CharSequence)
                || (!lastChunkHandlingString.equals(LOOSE)
                        && !lastChunkHandlingString.equals(STRICT)
                        && !lastChunkHandlingString.equals(STOP_BEFORE_PARTIAL))) {
            throw ScriptRuntime.typeErrorById("msg.bad.lastchunkhandling");
        }

        try {
            var result = Result.fromBase64(string, alphabetString, lastChunkHandlingString);
            if (result.error != null) {
                throw result.error;
            }

            var resultLength = result.bytes.length;
            var ta = js_constructor(cx, f, nt, s, thisObj, new Object[] {resultLength});
            ta.arrayBuffer.buffer = result.bytes;
            return ta;
        } catch (IOException exception) {
            throw ScriptRuntime.constructError("Error", "Error decoding base64");
        }
    }

    private static Object js_fromHex(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        if (!(isArg(args, 0) && args[0] instanceof CharSequence)) {
            throw ScriptRuntime.typeErrorById("msg.not.a.string");
        }

        var string = args[0].toString();

        var result = Result.fromHex(string);
        if (result.error != null) {
            throw result.error;
        }

        var resultLength = result.bytes.length;
        var ta = js_constructor(cx, f, nt, s, thisObj, new Object[] {resultLength});
        ta.arrayBuffer.buffer = result.bytes;
        return ta;
    }

    private static Object js_setFromBase64(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        var into = realThis(thisObj);

        if (!(isArg(args, 0) && args[0] instanceof CharSequence)) {
            throw ScriptRuntime.typeErrorById("msg.not.a.string");
        }

        var string = args[0].toString();
        var options = getOptionsObject(args, 1);

        var alphabet = ScriptableObject.getProperty(options, "alphabet");
        if (alphabet == NOT_FOUND || Undefined.isUndefined(alphabet)) {
            alphabet = BASE_64;
        }
        var alphabetString = alphabet.toString();

        if (!(alphabet instanceof CharSequence)
                || (!alphabetString.equals(BASE_64) && !alphabetString.equals(BASE_64_URL))) {
            throw ScriptRuntime.typeErrorById("msg.bad.alphabet");
        }

        var lastChunkHandling = ScriptableObject.getProperty(options, "lastChunkHandling");
        if (lastChunkHandling == NOT_FOUND || Undefined.isUndefined(lastChunkHandling)) {
            lastChunkHandling = LOOSE;
        }
        var lastChunkHandlingString = lastChunkHandling.toString();

        if (!(lastChunkHandling instanceof CharSequence)
                || (!lastChunkHandlingString.equals(LOOSE)
                        && !lastChunkHandlingString.equals(STRICT)
                        && !lastChunkHandlingString.equals(STOP_BEFORE_PARTIAL))) {
            throw ScriptRuntime.typeErrorById("msg.bad.lastchunkhandling");
        }

        var length = into.validateAndGetLength();

        try {
            var result = Result.fromBase64(string, alphabetString, lastChunkHandlingString, length);
            var bytes = result.bytes;

            for (int i = 0; i < bytes.length; i++) {
                into.js_set(i, bytes[i]);
            }

            if (result.error != null) {
                throw result.error;
            }

            var resultObj = cx.newObject(s);
            resultObj.put("read", resultObj, result.read);
            resultObj.put("written", resultObj, bytes.length);
            return resultObj;
        } catch (IOException exception) {
            throw ScriptRuntime.constructError("Error", "Error decoding base64");
        }
    }

    private static Object js_setFromHex(
            Context cx, JSFunction f, Object nt, VarScope s, Object thisObj, Object[] args) {
        var into = realThis(thisObj);

        if (!(isArg(args, 0) && args[0] instanceof CharSequence)) {
            throw ScriptRuntime.typeErrorById("msg.not.a.string");
        }

        var string = args[0].toString();
        var length = into.validateAndGetLength();

        var result = Result.fromHex(string, length);
        var bytes = result.bytes;

        for (int i = 0; i < bytes.length; i++) {
            into.js_set(i, bytes[i]);
        }

        if (result.error != null) {
            throw result.error;
        }

        var resultObj = cx.newObject(s);
        resultObj.put("read", resultObj, result.read);
        resultObj.put("written", resultObj, bytes.length);
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

    private static class Result {
        private final int read;
        private final byte[] bytes;
        private final EcmaError error;

        private Result(int read, byte[] bytes, EcmaError error) {
            this.read = read;
            this.bytes = bytes;
            this.error = error;
        }

        private static boolean isAsciiWhitespace(char c) {
            return c == '\t' || c == '\n' || c == '\f' || c == '\r' || c == ' ';
        }

        private static int skipWhitespace(String string, int index) {
            var length = string.length();
            while (index < length && isAsciiWhitespace(string.charAt(index))) {
                index++;
            }
            return index;
        }

        private static Result fromBase64(String string, String alphabet, String lastChunkHandling)
                throws IOException {
            return fromBase64(string, alphabet, lastChunkHandling, NativeNumber.MAX_SAFE_INTEGER);
        }

        private static Result fromBase64(
                String string, String alphabet, String lastChunkHandling, double maxLength)
                throws IOException {
            if (maxLength == 0) {
                return new Result(0, new byte[0], null);
            }

            var read = 0;
            var bytes = new ByteArrayOutputStream();
            var chunk = new StringBuilder();
            var index = 0;
            var length = string.length();

            while (true) {
                index = skipWhitespace(string, index);

                if (index == length) {
                    if (!chunk.isEmpty()) {
                        switch (lastChunkHandling) {
                            case STOP_BEFORE_PARTIAL:
                                return new Result(read, bytes.toByteArray(), null);
                            case STRICT:
                                return new Result(
                                        read,
                                        bytes.toByteArray(),
                                        ScriptRuntime.syntaxErrorById("msg.invalid.base64"));
                            case LOOSE:
                            default:
                                if (chunk.length() == 1) {
                                    return new Result(
                                            read,
                                            bytes.toByteArray(),
                                            ScriptRuntime.syntaxErrorById("msg.invalid.base64"));
                                }
                                bytes.write(DecodeFinalBase64Chunk(chunk, false));
                        }
                    }
                    return new Result(length, bytes.toByteArray(), null);
                }

                var c = string.charAt(index);
                index++;

                if (c == '=') {
                    if (chunk.length() < 2) {
                        return new Result(
                                read,
                                bytes.toByteArray(),
                                ScriptRuntime.syntaxErrorById("msg.invalid.base64"));
                    }

                    index = skipWhitespace(string, index);

                    if (chunk.length() == 2) {
                        if (index == length) {
                            if (lastChunkHandling.equals(STOP_BEFORE_PARTIAL)) {
                                return new Result(read, bytes.toByteArray(), null);
                            }
                            return new Result(
                                    read,
                                    bytes.toByteArray(),
                                    ScriptRuntime.syntaxError("msg.invalid.base64"));
                        }

                        c = string.charAt(index);
                        if (c == '=') {
                            index = skipWhitespace(string, index + 1);
                        }
                    }

                    if (index < length) {
                        return new Result(
                                read,
                                bytes.toByteArray(),
                                ScriptRuntime.syntaxError("msg.invalid.base64"));
                    }

                    var throwOnExtraBits = lastChunkHandling.equals(STRICT);
                    try {
                        bytes.write(DecodeFinalBase64Chunk(chunk, throwOnExtraBits));
                        return new Result(length, bytes.toByteArray(), null);
                    } catch (EcmaError error) {
                        return new Result(read, bytes.toByteArray(), error);
                    }
                }

                if (alphabet.equals(BASE_64_URL)) {
                    if (c == '+' || c == '/') {
                        return new Result(
                                read,
                                bytes.toByteArray(),
                                ScriptRuntime.syntaxError("msg.invalid.base64"));
                    } else if (c == '-') {
                        c = '+';
                    } else if (c == '_') {
                        c = '/';
                    }
                }

                if (!isBase64(c)) {
                    return new Result(
                            read,
                            bytes.toByteArray(),
                            ScriptRuntime.syntaxErrorById("msg.not.base64"));
                }

                var remaining = maxLength - bytes.size();
                if ((remaining == 1 && chunk.length() == 2)
                        || (remaining == 2 && chunk.length() == 3)) {
                    return new Result(read, bytes.toByteArray(), null);
                }

                chunk.append(c);

                if (chunk.length() == 4) {
                    bytes.write(DecodeFullBase64Chunk(chunk));
                    chunk.setLength(0);
                    read = index;

                    if (bytes.size() == maxLength) {
                        return new Result(read, bytes.toByteArray(), null);
                    }
                }
            }
        }

        private static boolean isBase64(char c) {
            return ('A' <= c && c <= 'Z')
                    || ('a' <= c && c <= 'z')
                    || ('0' <= c && c <= '9')
                    || (c == '+')
                    || (c == '/');
        }

        private static boolean isHex(char c) {
            return ('0' <= c && c <= '9') || ('A' <= c && c <= 'F') || ('a' <= c && c <= 'f');
        }

        private static byte[] DecodeFinalBase64Chunk(
                StringBuilder chunk, boolean throwOnExtraBits) {
            var chunkLength = chunk.length();
            if (chunkLength == 2) {
                chunk.append('A');
            }
            chunk.append('A');

            var bytes = DecodeFullBase64Chunk(chunk);

            if (chunkLength == 2) {
                if (throwOnExtraBits && bytes[1] != 0) {
                    throw ScriptRuntime.syntaxError("msg.invalid.base64");
                }
                return new byte[] {bytes[0]};
            }

            if (throwOnExtraBits && bytes[2] != 0) {
                throw ScriptRuntime.syntaxError("msg.invalid.base64");
            }
            return new byte[] {bytes[0], bytes[1]};
        }

        private static byte[] DecodeFullBase64Chunk(StringBuilder chunk) {
            return Base64.getDecoder().decode(chunk.toString().getBytes(StandardCharsets.UTF_8));
        }

        private static Result fromHex(String string) {
            return fromHex(string, NativeNumber.MAX_SAFE_INTEGER);
        }

        private static Result fromHex(String string, double maxLength) {
            var length = string.length();
            var bytes = new ByteArrayOutputStream();
            var read = 0;
            if (length % 2 != 0) {
                return new Result(
                        read,
                        bytes.toByteArray(),
                        ScriptRuntime.syntaxErrorById("msg.invalid.hex"));
            }

            while (read < length && bytes.size() < maxLength) {
                char c1 = string.charAt(read);
                char c2 = string.charAt(read + 1);
                if (!isHex(c1) || !isHex(c2)) {
                    return new Result(
                            read,
                            bytes.toByteArray(),
                            ScriptRuntime.syntaxErrorById("msg.invalid.hex"));
                }

                bytes.write((Character.digit(c1, 16) << 4) + Character.digit(c2, 16));
                read += 2;
            }

            return new Result(read, bytes.toByteArray(), null);
        }
    }

    @Override
    protected Object js_get(int index) {
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        return ByteIo.readUint8(arrayBuffer.buffer, index + offset);
    }

    @Override
    protected Object js_set(int index, Object c) {
        int val = Conversions.toUint8(c);
        if (checkIndex(index)) {
            return Undefined.instance;
        }
        ByteIo.writeUint8(arrayBuffer.buffer, index + offset, val);
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
