/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import java.util.Arrays;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.ScriptRuntime;

public final class HexCodec {
    private static final String ALPHABET = "0123456789abcdefABCDEF";
    private static final byte[] BYTE_TO_HEX_VALUE = buildByteToHexValue();

    private HexCodec() {}

    private static byte[] buildByteToHexValue() {
        var table = new byte[256];
        Arrays.fill(table, (byte) -1);
        for (var i = 0; i < ALPHABET.length(); i++) {
            var c = ALPHABET.charAt(i);
            table[c] = (byte) Character.digit(c, 16);
        }
        return table;
    }

    public static String encode(byte[] bytes) {
        var out = new char[bytes.length * 2];
        for (var i = 0; i < bytes.length; i++) {
            var b = bytes[i] & 0xff;
            out[i * 2] = ALPHABET.charAt(b >>> 4);
            out[i * 2 + 1] = ALPHABET.charAt(b & 0xf);
        }
        return new String(out);
    }

    public static DecodeResult decode(String string) {
        return decode(string, Integer.MAX_VALUE);
    }

    public static DecodeResult decode(String string, int maxLength) {
        var length = string.length();
        if (length % 2 != 0) {
            return new DecodeResult(
                    0, 0, new byte[0], ScriptRuntime.syntaxErrorById("msg.hex.not.even"));
        }

        var bytes = new byte[Math.min(length / 2, maxLength)];
        var read = 0;
        var written = 0;
        EcmaError error = null;

        while (written < bytes.length) {
            var c1 = string.charAt(read);
            var c2 = string.charAt(read + 1);

            byte v1;
            if (c1 >= BYTE_TO_HEX_VALUE.length || (v1 = BYTE_TO_HEX_VALUE[c1]) < 0) {
                error = ScriptRuntime.syntaxErrorById("msg.bad.hex", c1);
                break;
            }

            byte v2;
            if (c2 >= BYTE_TO_HEX_VALUE.length || (v2 = BYTE_TO_HEX_VALUE[c2]) < 0) {
                error = ScriptRuntime.syntaxErrorById("msg.bad.hex", c2);
                break;
            }

            bytes[written++] = (byte) ((v1 << 4) | v2);
            read += 2;
        }

        return new DecodeResult(read, written, bytes, error);
    }
}
