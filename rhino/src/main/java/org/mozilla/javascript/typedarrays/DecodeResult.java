/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.typedarrays;

import org.mozilla.javascript.EcmaError;

/**
 * The result of decoding a base64 or hex string into bytes, as returned by {@link
 * Base64Codec#decode} and {@link HexCodec#decode}.
 */
public final class DecodeResult {
    public final int read;
    public final int written;
    public final byte[] bytes;
    public final EcmaError error;

    /**
     * @param bytes the decoded bytes; may be longer than {@code written} since the buffer is sized
     *     to the longest possible result up front. Only the first {@code written} bytes are valid.
     */
    public DecodeResult(int read, int written, byte[] bytes, EcmaError error) {
        this.read = read;
        this.written = written;
        this.bytes = bytes;
        this.error = error;
    }
}
