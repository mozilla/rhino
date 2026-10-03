/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

load("testsrc/assert.js");

// Additional coverage for the es2026 Uint8Array base64/hex methods
// (fromBase64, fromHex, setFromBase64, setFromHex, toBase64, toHex),
// complementing the test262 suite rather than duplicating it.

(function TestBase64RoundTripAllByteValuesAllOptionCombos() {
  var bytes = new Uint8Array(256);
  for (var i = 0; i < 256; i++) {
    bytes[i] = i;
  }
  ["base64", "base64url"].forEach(function (alphabet) {
    [true, false].forEach(function (omitPadding) {
      var encoded = bytes.toBase64({ alphabet: alphabet, omitPadding: omitPadding });
      var decoded = Uint8Array.fromBase64(encoded, { alphabet: alphabet });
      assertArrayEquals(
          bytes, decoded, "roundtrip alphabet=" + alphabet + " omitPadding=" + omitPadding);

      var target = new Uint8Array(256);
      var result = target.setFromBase64(encoded, { alphabet: alphabet });
      assertEquals(256, result.written);
      assertArrayEquals(bytes, target, "setFromBase64 roundtrip alphabet=" + alphabet);
    });
  });
})();

(function TestEncodeFromOffsetSubarrayView() {
  var buf = new Uint8Array([0xaa, 0x01, 0x02, 0x03, 0xbb]);
  var view = buf.subarray(1, 4);

  assertEquals("010203", view.toHex());
  assertEquals("AQID", view.toBase64());

  // Encoding must not read or mutate bytes outside of the view.
  assertArrayEquals([0xaa, 0x01, 0x02, 0x03, 0xbb], buf);
})();

(function TestSetFromHexIntoOffsetSubarrayTruncated() {
  var base = new Uint8Array([255, 255, 255, 255, 255, 255, 255]);
  var sub = base.subarray(2, 4); // length 2, backed by base[2..3]

  var result = sub.setFromHex("aabbcc");
  assertEquals(4, result.read);
  assertEquals(2, result.written);
  assertArrayEquals([0xaa, 0xbb], sub);
  assertArrayEquals([255, 255, 0xaa, 0xbb, 255, 255, 255], base);
})();

(function TestSetFromBase64IntoOffsetSubarrayTruncated() {
  var base = new Uint8Array([255, 255, 255, 255, 255, 255, 255]);
  var sub = base.subarray(2, 5); // length 3, backed by base[2..4]

  var result = sub.setFromBase64("Zm9vYmFy"); // "foobar"
  assertEquals(4, result.read);
  assertEquals(3, result.written);
  assertArrayEquals([102, 111, 111], sub);
  assertArrayEquals([255, 255, 102, 111, 111, 255, 255], base);
})();

(function TestSetFromBase64ThrowsForNonUint8ArrayReceiver() {
  var badReceivers = [
    new Int8Array(3),
    new Uint16Array(3),
    new Uint8ClampedArray(3),
    new Float64Array(3),
    {},
    [],
  ];
  badReceivers.forEach(function (receiver) {
    assertThrows(function () {
      Uint8Array.prototype.setFromBase64.call(receiver, "Zg==");
    }, TypeError);
  });

  assertThrows(function () {
    var detachedMethod = Uint8Array.prototype.setFromBase64;
    detachedMethod("Zg==");
  }, TypeError);
})();

(function TestSetFromHexThrowsForNonUint8ArrayReceiver() {
  var badReceivers = [
    new Int8Array(3),
    new Uint16Array(3),
    new Uint8ClampedArray(3),
    new Float64Array(3),
    {},
    [],
  ];
  badReceivers.forEach(function (receiver) {
    assertThrows(function () {
      Uint8Array.prototype.setFromHex.call(receiver, "aa");
    }, TypeError);
  });

  assertThrows(function () {
    var detachedMethod = Uint8Array.prototype.setFromHex;
    detachedMethod("aa");
  }, TypeError);
})();

(function TestWhitespaceCombinedWithAlphabetAndPadding() {
  // "x-8=" is the base64url (padded) encoding of [199, 239]; interleave
  // every kind of ASCII whitespace around its characters and padding.
  var withWhitespace = " x\t-\n8\f =\r ";

  var fromStatic = Uint8Array.fromBase64(withWhitespace, { alphabet: "base64url" });
  assertArrayEquals([199, 239], fromStatic);

  var target = new Uint8Array([255, 255, 255]);
  var result = target.setFromBase64(withWhitespace, { alphabet: "base64url" });
  assertEquals(withWhitespace.length, result.read);
  assertEquals(2, result.written);
  assertArrayEquals([199, 239, 255], target);
})();

(function TestStopBeforePartialIgnoresTrailingWhitespace() {
  // An unpadded final chunk followed by trailing whitespace should still be
  // recognized as partial, and therefore excluded, under stop-before-partial.
  var result = Uint8Array.fromBase64("ZXhhZg \t\n", { lastChunkHandling: "stop-before-partial" });
  assertArrayEquals([101, 120, 97], result);
})();

(function TestLastChunkHandlingAgreesWhenFullyPaddedAndClean() {
  // [1, 2, 3, 4, 5] encodes to a properly-padded, bit-clean string, so all
  // three lastChunkHandling modes must produce the identical, full result.
  var padded = "AQIDBAU=";
  ["loose", "strict", "stop-before-partial"].forEach(function (mode) {
    var result = Uint8Array.fromBase64(padded, { lastChunkHandling: mode });
    assertArrayEquals([1, 2, 3, 4, 5], result, "mode=" + mode);
  });
})();

(function TestLastChunkHandlingDivergesWhenUnpadded() {
  // The same bytes without the trailing "=": loose fills in the missing
  // padding, strict rejects it outright, and stop-before-partial decodes
  // only the complete 4-character groups that precede the partial one.
  var unpadded = "AQIDBAU";

  assertArrayEquals(
      [1, 2, 3, 4, 5], Uint8Array.fromBase64(unpadded, { lastChunkHandling: "loose" }));

  assertThrows(function () {
    Uint8Array.fromBase64(unpadded, { lastChunkHandling: "strict" });
  }, SyntaxError);

  assertArrayEquals(
      [1, 2, 3],
      Uint8Array.fromBase64(unpadded, { lastChunkHandling: "stop-before-partial" }));
})();

(function TestLastChunkHandlingDivergesOnSetFromBase64() {
  // Same unpadded input as above, but through setFromBase64, which also
  // reports read/written counts and writes partial progress before throwing.
  var unpadded = "AQIDBAU";

  var looseTarget = new Uint8Array([255, 255, 255, 255, 255]);
  var looseResult = looseTarget.setFromBase64(unpadded, { lastChunkHandling: "loose" });
  assertEquals(7, looseResult.read);
  assertEquals(5, looseResult.written);
  assertArrayEquals([1, 2, 3, 4, 5], looseTarget);

  var strictTarget = new Uint8Array([255, 255, 255, 255, 255]);
  assertThrows(function () {
    strictTarget.setFromBase64(unpadded, { lastChunkHandling: "strict" });
  }, SyntaxError);
  // The complete leading group is written before the error is thrown.
  assertArrayEquals([1, 2, 3, 255, 255], strictTarget);

  var stopTarget = new Uint8Array([255, 255, 255, 255, 255]);
  var stopResult = stopTarget.setFromBase64(unpadded, {
    lastChunkHandling: "stop-before-partial",
  });
  assertEquals(4, stopResult.read);
  assertEquals(3, stopResult.written);
  assertArrayEquals([1, 2, 3, 255, 255], stopTarget);
})();

(function TestLastChunkHandlingDivergesOnlyForStrictWhenPaddedWithGarbageBits() {
  // Same bytes, but the character just before the padding is swapped for one
  // whose unused low bits are non-zero ('U' -> 'V'). Those bits don't belong
  // to any output byte, so loose and stop-before-partial both silently
  // discard them and decode normally; only strict rejects the input.
  var paddedWithGarbageBits = "AQIDBAV=";

  assertArrayEquals(
      [1, 2, 3, 4, 5],
      Uint8Array.fromBase64(paddedWithGarbageBits, { lastChunkHandling: "loose" }));

  assertThrows(function () {
    Uint8Array.fromBase64(paddedWithGarbageBits, { lastChunkHandling: "strict" });
  }, SyntaxError);

  assertArrayEquals(
      [1, 2, 3, 4, 5],
      Uint8Array.fromBase64(paddedWithGarbageBits, { lastChunkHandling: "stop-before-partial" }));
})();

(function TestEmptyStringInputsProduceZeroLengthArrays() {
  assertEquals(0, Uint8Array.fromBase64("").length);
  assertEquals(0, Uint8Array.fromHex("").length);
  assertEquals("", new Uint8Array(0).toBase64());
  assertEquals("", new Uint8Array(0).toHex());

  var target = new Uint8Array(0);
  var base64Result = target.setFromBase64("");
  assertEquals(0, base64Result.read);
  assertEquals(0, base64Result.written);

  var hexResult = target.setFromHex("");
  assertEquals(0, hexResult.read);
  assertEquals(0, hexResult.written);
})();

"success";
