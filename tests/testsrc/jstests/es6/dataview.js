/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

load("testsrc/assert.js");

(function TestSymbolSpecies() {
  var symbolSpeciesValue = DataView[Symbol.species];
  assertEquals(undefined, symbolSpeciesValue);
})();

(function TestSymbolToString() {
  var ab = new ArrayBuffer(256);
  var d = new DataView(ab, 1, 255);;

  assertEquals('DataView', d[Symbol.toStringTag]);
  assertEquals(false, d.hasOwnProperty(Symbol.toStringTag));
  assertEquals(false, DataView.hasOwnProperty(Symbol.toStringTag));
  assertEquals(true, DataView.prototype.hasOwnProperty(Symbol.toStringTag));
})();

(function TestBigInt64GetAndSet() {
  function t(origVal, expectedSigned, expectedUnsigned) {
    const buf = new ArrayBuffer(8);
    const v = new DataView(buf);
    v.setBigInt64(0, origVal);
    const newSignedVal = v.getBigInt64(0);
    assertEquals(expectedSigned, newSignedVal);
    v.setBigUint64(0, origVal);
    const newUnsignedVal = v.getBigUint64(0);
    assertEquals(expectedUnsigned, newUnsignedVal);
  }

  t(0n, 0n, 0n);
  t(1n, 1n, 1n);

  // A large value that still fits in a signed 64-bit value
  t(1234567890123456789n, 1234567890123456789n, 1234567890123456789n);

  // Just over a 64-bit signed value, wraps around to -1, fits in unsigned
  t(18446744073709551615n, -1n, 18446744073709551615n);

  // Very much too large for 64 bits, wraps around
  t(9223372036854775807n + 1n, -9223372036854775808n,  9223372036854775808n);

  // Wraps around in the other direction,
  // when unsigned simply has less precision
  t(-9223372036854775808n - 1n, 9223372036854775807n,  9223372036854775807n);
})();

"success";
