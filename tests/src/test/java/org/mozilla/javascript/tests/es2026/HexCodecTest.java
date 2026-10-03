/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es2026;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.typedarrays.DecodeResult;
import org.mozilla.javascript.typedarrays.HexCodec;

public class HexCodecTest {

    private static byte[] trim(DecodeResult result) {
        var trimmed = new byte[result.written];
        System.arraycopy(result.bytes, 0, trimmed, 0, trimmed.length);
        return trimmed;
    }

    @Test
    public void encodeProducesLowercaseHex() {
        var bytes = new byte[] {0x00, (byte) 0xff, (byte) 0xab, 0x0a};
        assertEquals("00ffab0a", HexCodec.encode(bytes));
    }

    @Test
    public void encodeEmptyArrayProducesEmptyString() {
        assertEquals("", HexCodec.encode(new byte[0]));
    }

    @Test
    public void decodeAcceptsLowercaseAndUppercaseDigits() {
        var lower = HexCodec.decode("00ffab0a");
        assertNull(lower.error);
        assertArrayEquals(new byte[] {0x00, (byte) 0xff, (byte) 0xab, 0x0a}, trim(lower));

        var upper = HexCodec.decode("00FFAB0A");
        assertNull(upper.error);
        assertArrayEquals(new byte[] {0x00, (byte) 0xff, (byte) 0xab, 0x0a}, trim(upper));
    }

    @Test
    public void decodeRoundTripsEncodedBytes() {
        var bytes = new byte[] {0x00, (byte) 0xff, 0x10, 0x2a, (byte) 0x80};
        var result = HexCodec.decode(HexCodec.encode(bytes));
        assertNull(result.error);
        assertArrayEquals(bytes, trim(result));
        assertEquals(bytes.length, result.written);
    }

    @Test
    public void decodeEmptyStringProducesEmptyResult() {
        var result = HexCodec.decode("");
        assertNull(result.error);
        assertEquals(0, result.written);
        assertEquals(0, result.read);
    }

    @Test
    public void decodeRejectsOddLengthString() {
        var result = HexCodec.decode("abc");
        assertInstanceOf(EcmaError.class, result.error);
        assertEquals(0, result.written);
        assertEquals(0, result.read);
    }

    @Test
    public void decodeRejectsOddLengthStringWithAFreshErrorEachTime() {
        // The error must not be a cached/shared instance: each caller's exception should
        // reflect its own call site rather than whichever call happened to construct it first.
        var first = HexCodec.decode("abc");
        var second = HexCodec.decode("abc");
        assertNotSame(first.error, second.error);
    }

    @Test
    public void decodeRejectsInvalidFirstCharacterOfPair() {
        var result = HexCodec.decode("zz");
        assertInstanceOf(EcmaError.class, result.error);
    }

    @Test
    public void decodeRejectsInvalidSecondCharacterOfPair() {
        var result = HexCodec.decode("0z");
        assertInstanceOf(EcmaError.class, result.error);
        assertEquals(0, result.read);
        assertEquals(0, result.written);
    }

    @Test
    public void decodeReportsFirstInvalidCharacterAfterValidPairs() {
        var result = HexCodec.decode("00ffzz");
        assertNotNull(result.error);
        assertEquals(4, result.read);
        assertEquals(2, result.written);
        assertArrayEquals(new byte[] {0x00, (byte) 0xff}, trim(result));
    }

    @Test
    public void decodeHonorsMaxLength() {
        var result = HexCodec.decode("00ffab0a", 2);
        assertNull(result.error);
        assertEquals(2, result.written);
        assertEquals(4, result.read);
        assertArrayEquals(new byte[] {0x00, (byte) 0xff}, trim(result));
    }

    @Test
    public void decodeWithMaxLengthZeroReturnsEmptyResult() {
        var result = HexCodec.decode("00ffab0a", 0);
        assertNull(result.error);
        assertEquals(0, result.written);
        assertEquals(0, result.read);
    }
}
