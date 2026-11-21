// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

load("testsrc/assert.js");

// --- BigInt64Array basic read/write ---
(function TestBigInt64ArrayBasic() {
  var a = new BigInt64Array(4);
  assertEquals(4, a.length);
  assertEquals(8, a.BYTES_PER_ELEMENT);

  a[0] = 0n;
  a[1] = -1n;
  a[2] = 9223372036854775807n;  // INT64_MAX = (2n**63n) - 1n
  a[3] = -9223372036854775808n; // INT64_MIN = -(2n**63n)

  assertEquals(0n, a[0]);
  assertEquals(-1n, a[1]);
  assertEquals(9223372036854775807n, a[2]);
  assertEquals(-9223372036854775808n, a[3]);
})();

// --- BigInt64Array overflow/wrap-around ---
(function TestBigInt64ArrayOverflow() {
  var a = new BigInt64Array(1);
  // Storing INT64_MAX + 1 wraps to INT64_MIN
  a[0] = 9223372036854775808n; // 2n**63n
  assertEquals(-9223372036854775808n, a[0]);

  // Storing -INT64_MIN wraps to INT64_MIN (two's complement)
  a[0] = -9223372036854775809n; // -(2n**63n) - 1n
  assertEquals(9223372036854775807n, a[0]);
})();

// --- BigInt64Array TypeError on non-BigInt assignment ---
(function TestBigInt64ArrayTypeError() {
  var a = new BigInt64Array(2);
  assertThrows(function() { a[0] = 1; }, TypeError);
  assertThrows(function() { a[0] = "1"; }, TypeError);
  assertThrows(function() { a[0] = true; }, TypeError);
})();

// --- BigUint64Array basic read/write ---
(function TestBigUint64ArrayBasic() {
  var a = new BigUint64Array(4);
  assertEquals(4, a.length);
  assertEquals(8, a.BYTES_PER_ELEMENT);

  a[0] = 0n;
  a[1] = 1n;
  a[2] = 18446744073709551615n; // UINT64_MAX = (2n**64n) - 1n
  a[3] = 9223372036854775808n;  // 2n**63n

  assertEquals(0n, a[0]);
  assertEquals(1n, a[1]);
  assertEquals(18446744073709551615n, a[2]);
  assertEquals(9223372036854775808n, a[3]);
})();

// --- BigUint64Array overflow/wrap-around ---
(function TestBigUint64ArrayOverflow() {
  var a = new BigUint64Array(1);
  // UINT64_MAX + 1 wraps to 0
  a[0] = 18446744073709551616n; // 2n**64n
  assertEquals(0n, a[0]);

  // -1n wraps to UINT64_MAX
  a[0] = -1n;
  assertEquals(18446744073709551615n, a[0]);
})();

// --- BigUint64Array TypeError on non-BigInt assignment ---
(function TestBigUint64ArrayTypeError() {
  var a = new BigUint64Array(2);
  assertThrows(function() { a[0] = 1; }, TypeError);
  assertThrows(function() { a[0] = "1"; }, TypeError);
  assertThrows(function() { a[0] = true; }, TypeError);
})();

// --- Construction from ArrayBuffer with byteOffset ---
(function TestBigInt64ArrayFromBuffer() {
  var buf = new ArrayBuffer(32);
  var a = new BigInt64Array(buf, 8, 2);
  assertEquals(2, a.length);
  assertEquals(8, a.byteOffset);
  a[0] = 42n;
  a[1] = -42n;
  assertEquals(42n, a[0]);
  assertEquals(-42n, a[1]);
})();

(function TestBigUint64ArrayFromBuffer() {
  var buf = new ArrayBuffer(32);
  var a = new BigUint64Array(buf, 16, 2);
  assertEquals(2, a.length);
  assertEquals(16, a.byteOffset);
  a[0] = 100n;
  a[1] = 200n;
  assertEquals(100n, a[0]);
  assertEquals(200n, a[1]);
})();

// --- DataView getBigInt64 / setBigInt64 ---
(function TestDataViewBigInt64() {
  var buf = new ArrayBuffer(16);
  var view = new DataView(buf);

  // big-endian (default)
  view.setBigInt64(0, 1234567890123456789n);
  assertEquals(1234567890123456789n, view.getBigInt64(0));

  // little-endian round-trip
  view.setBigInt64(0, -9876543210987654321n, true);
  assertEquals(-9876543210987654321n, view.getBigInt64(0, true));

  // endianness cross-check: big-endian write, read as little-endian must differ
  view.setBigInt64(0, 0x0102030405060708n, false);
  assertEquals(0x0807060504030201n, view.getBigInt64(0, true));

  // negative value
  view.setBigInt64(0, -1n);
  assertEquals(-1n, view.getBigInt64(0));
})();

// --- DataView getBigUint64 / setBigUint64 ---
(function TestDataViewBigUint64() {
  var buf = new ArrayBuffer(16);
  var view = new DataView(buf);

  // big-endian (default)
  view.setBigUint64(0, 18446744073709551615n);
  assertEquals(18446744073709551615n, view.getBigUint64(0));

  // little-endian round-trip
  view.setBigUint64(0, 9999999999999999999n, true);
  assertEquals(9999999999999999999n, view.getBigUint64(0, true));

  // endianness cross-check
  view.setBigUint64(0, 0x0102030405060708n, false);
  assertEquals(0x0807060504030201n, view.getBigUint64(0, true));

  // zero
  view.setBigUint64(0, 0n);
  assertEquals(0n, view.getBigUint64(0));
})();

// --- DataView out-of-bounds RangeError ---
(function TestDataViewOutOfBounds() {
  var buf = new ArrayBuffer(8);
  var view = new DataView(buf);

  // offset 1 for a 8-byte read goes out of bounds
  assertThrows(function() { view.getBigInt64(1); }, RangeError);
  assertThrows(function() { view.getBigUint64(1); }, RangeError);
  assertThrows(function() { view.setBigInt64(1, 0n); }, RangeError);
  assertThrows(function() { view.setBigUint64(1, 0n); }, RangeError);

  // negative offset
  assertThrows(function() { view.getBigInt64(-1); }, RangeError);
  assertThrows(function() { view.getBigUint64(-1); }, RangeError);
})();

"success";
