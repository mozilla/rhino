/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es2026;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.typedarrays.Base64Codec;
import org.mozilla.javascript.typedarrays.DecodeResult;

public class Base64CodecTest {

    private static byte[] bytesOf(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    @Test
    public void encodeProducesRfc4648TestVectors() {
        assertEquals("", Base64Codec.encode(bytesOf(""), false, false));
        assertEquals("Zg==", Base64Codec.encode(bytesOf("f"), false, false));
        assertEquals("Zm8=", Base64Codec.encode(bytesOf("fo"), false, false));
        assertEquals("Zm9v", Base64Codec.encode(bytesOf("foo"), false, false));
        assertEquals("Zm9vYg==", Base64Codec.encode(bytesOf("foob"), false, false));
        assertEquals("Zm9vYmE=", Base64Codec.encode(bytesOf("fooba"), false, false));
        assertEquals("Zm9vYmFy", Base64Codec.encode(bytesOf("foobar"), false, false));
    }

    @Test
    public void encodeCanOmitPadding() {
        assertEquals("Zg", Base64Codec.encode(bytesOf("f"), false, true));
        assertEquals("Zm8", Base64Codec.encode(bytesOf("fo"), false, true));
        assertEquals("Zm9v", Base64Codec.encode(bytesOf("foo"), false, true));
    }

    @Test
    public void encodeUsesUrlAlphabetInsteadOfPlusAndSlash() {
        var bytes = new byte[] {(byte) 0xfb, (byte) 0xf0};
        assertEquals("+/A=", Base64Codec.encode(bytes, false, false));
        assertEquals("-_A=", Base64Codec.encode(bytes, true, false));
    }

    @Test
    public void decodeProducesRfc4648TestVectors() {
        assertArrayEquals(bytesOf("f"), decodedBytes("Zg==", false));
        assertArrayEquals(bytesOf("fo"), decodedBytes("Zm8=", false));
        assertArrayEquals(bytesOf("foo"), decodedBytes("Zm9v", false));
        assertArrayEquals(bytesOf("foob"), decodedBytes("Zm9vYg==", false));
        assertArrayEquals(bytesOf("fooba"), decodedBytes("Zm9vYmE=", false));
        assertArrayEquals(bytesOf("foobar"), decodedBytes("Zm9vYmFy", false));
    }

    private static byte[] decodedBytes(String encoded, boolean isBase64Url) {
        var result = Base64Codec.decode(encoded, isBase64Url, Base64Codec.LOOSE);
        assertNull(result.error());
        return trim(result);
    }

    private static byte[] trim(DecodeResult result) {
        var trimmed = new byte[result.written()];
        System.arraycopy(result.bytes(), 0, trimmed, 0, trimmed.length);
        return trimmed;
    }

    @Test
    public void decodeSkipsAsciiWhitespace() {
        var result = Base64Codec.decode(" Zm9v\tYmFy\n", false, Base64Codec.LOOSE);
        assertNull(result.error());
        assertArrayEquals(bytesOf("foobar"), trim(result));
        assertEquals(11, result.read());
    }

    @Test
    public void decodeRoundTripsUrlAlphabet() {
        var bytes = new byte[] {(byte) 0xfb, (byte) 0xef, 0x3c, 0x00, 0x7f};
        var encoded = Base64Codec.encode(bytes, true, false);
        var result = Base64Codec.decode(encoded, true, Base64Codec.LOOSE);
        assertNull(result.error());
        assertArrayEquals(bytes, trim(result));
    }

    @Test
    public void decodeRejectsPlusAndSlashWhenUrlAlphabetRequested() {
        var result = Base64Codec.decode("+", true, Base64Codec.LOOSE);
        assertInstanceOf(EcmaError.class, result.error());
    }

    @Test
    public void decodeRejectsInvalidCharacter() {
        var result = Base64Codec.decode("Zm9v!", false, Base64Codec.LOOSE);
        assertInstanceOf(EcmaError.class, result.error());
        // The leading, complete "Zm9v" ("foo") group was still consumed before the error.
        assertEquals(4, result.read());
        assertEquals(3, result.written());
        assertArrayEquals(bytesOf("foo"), trim(result));
    }

    @Test
    public void decodeStrictAcceptsCanonicallyPaddedPartialChunk() {
        // "Zg==" decodes to "f": the two padding bits in the final sextet are zero, so strict
        // mode has nothing to reject.
        var result = Base64Codec.decode("Zg==", false, Base64Codec.STRICT);
        assertNull(result.error());
        assertArrayEquals(bytesOf("f"), trim(result));
        assertEquals(4, result.read());
    }

    @Test
    public void decodeStrictRejectsNonCanonicalPaddingBits() {
        // "TR==" decodes 'T' and 'R' into a 2-character group whose padding sextet has
        // non-zero low bits, which strict mode must reject even though it is properly padded.
        var result = Base64Codec.decode("TR==", false, Base64Codec.STRICT);
        assertInstanceOf(EcmaError.class, result.error());
        // The malformed group produced no output at all.
        assertEquals(0, result.read());
        assertEquals(0, result.written());
    }

    @Test
    public void decodeLooseAcceptsNonCanonicalPaddingBits() {
        // The same non-canonical group decodes fine outside of strict mode.
        var result = Base64Codec.decode("TR==", false, Base64Codec.LOOSE);
        assertNull(result.error());
        assertEquals(1, result.written());
        assertEquals(4, result.read());
    }

    @Test
    public void decodeRejectsDanglingSingleCharacterInLooseMode() {
        // "Zm9v" decodes to "foo", leaving a single dangling 'Y' that can never form a valid
        // group on its own, even in loose mode.
        var result = Base64Codec.decode("Zm9vY", false, Base64Codec.LOOSE);
        assertNotNull(result.error());
        assertEquals(4, result.read());
        assertEquals(3, result.written());
        assertArrayEquals(bytesOf("foo"), trim(result));
    }

    @Test
    public void decodeStopBeforePartialSilentlyDropsDanglingSingleCharacter() {
        var result = Base64Codec.decode("Zm9v1", false, Base64Codec.STOP_BEFORE_PARTIAL);
        assertNull(result.error());
        // The dangling '1' never joins a complete group, so it contributes nothing and is not
        // reflected in "read" even though it was scanned past.
        assertArrayEquals(bytesOf("foo"), trim(result));
        assertEquals(4, result.read());
    }

    @Test
    public void decodeRejectsMisplacedPaddingCharacter() {
        // A single leading character followed directly by '=' can never be a valid group.
        var result = Base64Codec.decode("Z=", false, Base64Codec.LOOSE);
        assertNotNull(result.error());
        assertEquals(0, result.read());
        assertEquals(0, result.written());
    }

    @Test
    public void decodeMaxLengthStopsBeforeGroupThatWouldProduceTooManyBytes() {
        // After "Zm9v" (3 bytes written), only 1 byte of room remains. The next group "YgF"
        // would produce 2 bytes once its 3rd character is read, so decoding must stop right
        // before that 3rd character is even appended to the pending group.
        var result = Base64Codec.decode("Zm9vYgF", false, Base64Codec.LOOSE, 4);
        assertNull(result.error());
        assertEquals(3, result.written());
        assertEquals(4, result.read());
        assertArrayEquals(bytesOf("foo"), trim(result));
    }

    @Test
    public void decodeMaxLengthStopsBeforeFourthCharacterThatWouldOverflow() {
        // After "Zm9v" (3 bytes written), 2 bytes of room remain. The next group "YgFy" would
        // produce 3 bytes once its 4th character is read, so decoding must stop right before
        // that 4th character is appended.
        var result = Base64Codec.decode("Zm9vYgFy", false, Base64Codec.LOOSE, 5);
        assertNull(result.error());
        assertEquals(3, result.written());
        assertEquals(4, result.read());
        assertArrayEquals(bytesOf("foo"), trim(result));
    }

    @Test
    public void decodeLooseAllowsUnpaddedTrailingPartialChunk() {
        // "Zm9vYg" is "foob" without its trailing "==" padding.
        var result = Base64Codec.decode("Zm9vYg", false, Base64Codec.LOOSE);
        assertNull(result.error());
        assertArrayEquals(bytesOf("foob"), trim(result));
        assertEquals(6, result.read());
    }

    @Test
    public void decodeStrictRejectsUnpaddedTrailingPartialChunk() {
        var result = Base64Codec.decode("Zm9vYg", false, Base64Codec.STRICT);
        assertNotNull(result.error());
        // Only the leading, complete "Zm9v" ("foo") group was consumed before the error.
        assertEquals(4, result.read());
        assertEquals(3, result.written());
        assertArrayEquals(bytesOf("foo"), trim(result));
    }

    @Test
    public void decodeStopBeforePartialStopsAtIncompleteTrailingChunk() {
        var result = Base64Codec.decode("Zm9vYg", false, Base64Codec.STOP_BEFORE_PARTIAL);
        assertNull(result.error());
        assertArrayEquals(bytesOf("foo"), trim(result));
        assertEquals(4, result.read());
    }

    @Test
    public void decodeHonorsMaxLength() {
        var result = Base64Codec.decode("Zm9vYmFy", false, Base64Codec.LOOSE, 3);
        assertNull(result.error());
        assertEquals(3, result.written());
        assertEquals(4, result.read());
        assertArrayEquals(bytesOf("foo"), trim(result));
    }

    @Test
    public void decodeWithMaxLengthZeroReturnsEmptyResult() {
        var result = Base64Codec.decode("Zm9vYmFy", false, Base64Codec.LOOSE, 0);
        assertNull(result.error());
        assertEquals(0, result.written());
        assertEquals(0, result.read());
    }

    @Test
    public void decodeEmptyStringProducesEmptyResult() {
        var result = Base64Codec.decode("", false, Base64Codec.LOOSE);
        assertNull(result.error());
        assertEquals(0, result.written());
        assertEquals(0, result.read());
    }
}
