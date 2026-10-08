/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import java.util.Arrays;
import java.util.Base64;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.ScriptRuntime;

public final class Base64Codec {
    public static final String BASE_64 = "base64";
    public static final String BASE_64_URL = "base64url";

    public static final String LOOSE = "loose";
    public static final String STRICT = "strict";
    public static final String STOP_BEFORE_PARTIAL = "stop-before-partial";

    private static final String ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private static final byte[] BYTE_TO_BASE64_VALUE = buildByteToBase64Value();

    private static final DecodeResult EMPTY_RESULT = new DecodeResult(0, 0, new byte[0], null);

    private Base64Codec() {}

    private static byte[] buildByteToBase64Value() {
        var table = new byte[256];
        Arrays.fill(table, (byte) -1);
        for (var i = 0; i < ALPHABET.length(); i++) {
            table[ALPHABET.charAt(i)] = (byte) i;
        }
        return table;
    }

    public static String encode(byte[] bytes, boolean isBase64Url, boolean omitPadding) {
        var encoder = isBase64Url ? Base64.getUrlEncoder() : Base64.getEncoder();
        if (omitPadding) {
            encoder = encoder.withoutPadding();
        }
        return encoder.encodeToString(bytes);
    }

    public static DecodeResult decode(
            String string, boolean isBase64Url, String lastChunkHandling) {
        return decode(string, isBase64Url, lastChunkHandling, Integer.MAX_VALUE);
    }

    public static DecodeResult decode(
            String string, boolean isBase64Url, String lastChunkHandling, int maxLength) {
        if (maxLength == 0) {
            return EMPTY_RESULT;
        }

        var read = 0;
        var length = string.length();
        var bytes = new byte[Math.min(estimateDecodedLength(string, length), maxLength)];
        var written = 0;
        var chunk = new char[4];
        var chunkLength = 0;
        var index = 0;
        EcmaError error = null;

        decodeLoop:
        while (true) {
            index = skipWhitespace(string, index);

            if (index == length) {
                if (chunkLength > 0) {
                    switch (lastChunkHandling) {
                        case STOP_BEFORE_PARTIAL:
                            break decodeLoop;
                        case STRICT:
                            error = ScriptRuntime.syntaxErrorById("msg.invalid.base64");
                            break decodeLoop;
                        case LOOSE:
                        default:
                            if (chunkLength == 1) {
                                error = ScriptRuntime.syntaxErrorById("msg.invalid.base64");
                                break decodeLoop;
                            }
                            written += decodeChunk(chunk, chunkLength, bytes, written, false);
                    }
                }
                read = length;
                break;
            }

            var c = string.charAt(index);
            index++;

            if (c == '=') {
                if (chunkLength < 2) {
                    error = ScriptRuntime.syntaxErrorById("msg.invalid.base64");
                    break;
                }

                index = skipWhitespace(string, index);

                if (chunkLength == 2) {
                    if (index == length) {
                        if (lastChunkHandling.equals(STOP_BEFORE_PARTIAL)) {
                            break;
                        }
                        error = ScriptRuntime.syntaxErrorById("msg.invalid.base64");
                        break;
                    }

                    c = string.charAt(index);
                    if (c == '=') {
                        index = skipWhitespace(string, index + 1);
                    }
                }

                if (index < length) {
                    error = ScriptRuntime.syntaxErrorById("msg.invalid.base64");
                    break;
                }

                try {
                    written +=
                            decodeChunk(
                                    chunk,
                                    chunkLength,
                                    bytes,
                                    written,
                                    lastChunkHandling.equals(STRICT));
                    read = length;
                } catch (EcmaError e) {
                    error = e;
                }
                break;
            }

            if (isBase64Url) {
                if (c == '+' || c == '/') {
                    error = ScriptRuntime.syntaxErrorById("msg.not.base64", c);
                    break;
                } else if (c == '-') {
                    c = '+';
                } else if (c == '_') {
                    c = '/';
                }
            }

            if (!isBase64(c)) {
                error = ScriptRuntime.syntaxErrorById("msg.not.base64", c);
                break;
            }

            var remaining = maxLength - written;
            if ((remaining == 1 && chunkLength == 2) || (remaining == 2 && chunkLength == 3)) {
                break;
            }

            chunk[chunkLength++] = c;

            if (chunkLength == 4) {
                written += decodeChunk(chunk, chunkLength, bytes, written, false);
                chunkLength = 0;
                read = index;

                if (written == maxLength) {
                    break;
                }
            }
        }

        return new DecodeResult(read, written, bytes, error);
    }

    private static int estimateDecodedLength(String string, int length) {
        if (length < 2) {
            return 0;
        }

        var decoded = 3 * (length / 4);
        if (length % 4 == 0) {
            if (string.charAt(length - 1) == '=') {
                decoded--;
                if (string.charAt(length - 2) == '=') {
                    decoded--;
                }
            }
        } else {
            decoded += length % 4 - 1;
        }
        return decoded;
    }

    private static boolean isBase64(char c) {
        return c < BYTE_TO_BASE64_VALUE.length && BYTE_TO_BASE64_VALUE[c] >= 0;
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

    private static int decodeChunk(
            char[] chunk, int chunkLength, byte[] bytes, int bytesBegin, boolean throwOnExtraBits) {
        int written;
        switch (chunkLength) {
            case 2:
                chunk[2] = 'A';
                chunk[3] = 'A';
                written = 1;
                break;
            case 3:
                chunk[3] = 'A';
                written = 2;
                break;
            default:
                written = 3;
        }

        var v0 = BYTE_TO_BASE64_VALUE[chunk[0]];
        var v1 = BYTE_TO_BASE64_VALUE[chunk[1]];
        var v2 = BYTE_TO_BASE64_VALUE[chunk[2]];
        var v3 = BYTE_TO_BASE64_VALUE[chunk[3]];
        var b0 = (byte) ((v0 << 2) | (v1 >> 4));
        var b1 = (byte) (((v1 & 0xF) << 4) | (v2 >> 2));
        var b2 = (byte) (((v2 & 0x3) << 6) | v3);

        if (throwOnExtraBits && ((chunkLength == 2 && b1 != 0) || (chunkLength == 3 && b2 != 0))) {
            throw ScriptRuntime.syntaxErrorById("msg.invalid.base64");
        }

        bytes[bytesBegin] = b0;
        if (written > 1) {
            bytes[bytesBegin + 1] = b1;
        }
        if (written > 2) {
            bytes[bytesBegin + 2] = b2;
        }
        return written;
    }
}
