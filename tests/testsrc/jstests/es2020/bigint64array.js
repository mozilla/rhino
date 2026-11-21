// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

load("testsrc/assert.js");

// BigInt64Array basic tests
(function TestBigInt64ArrayBasic() {
  var a = new BigInt64Array(4);
  assertEquals(4, a.length);
  assertEquals(8, a.BYTES_PER_ELEMENT);
  assertEquals(32, a.byteLength);

  a[0] = 1n;
  a[1] = -1n;
  a[2] = 9223372036854775807n;  // BigInt.asIntN(64, -1n >> 1n) max
  a[3] = -9223372036854775808n; // min
  assertEquals(1n, a[0]);
  assertEquals(-1n, a[1]);
  assertEquals(9223372036854775807n, a[2]);
  assertEquals(-9223372036854775808n, a[3]);
})();

// BigUint64Array basic tests
(function TestBigUint64ArrayBasic() {
  var a = new BigUint64Array(4);
  assertEquals(4, a.length);
  assertEquals(8, a.BYTES_PER_ELEMENT);
  assertEquals(32, a.byteLength);

  a[0] = 0n;
  a[1] = 1n;
  a[2] = 18446744073709551615n; // 2n**64n - 1n max
  a[3] = 42n;
  assertEquals(0n, a[0]);
  assertEquals(1n, a[1]);
  assertEquals(18446744073709551615n, a[2]);
  assertEquals(42n, a[3]);
})();

// DataView getBigInt64 / setBigInt64
(function TestDataViewBigInt64() {
  var buf = new ArrayBuffer(16);
  var view = new DataView(buf);

  view.setBigInt64(0, 1n);
  assertEquals(1n, view.getBigInt64(0));

  view.setBigInt64(0, -1n);
  assertEquals(-1n, view.getBigInt64(0));

  view.setBigInt64(0, 9223372036854775807n);
  assertEquals(9223372036854775807n, view.getBigInt64(0));

  view.setBigInt64(0, -9223372036854775808n);
  assertEquals(-9223372036854775808n, view.getBigInt64(0));

  // little-endian
  view.setBigInt64(0, 1n, true);
  assertEquals(1n, view.getBigInt64(0, true));
})();

// DataView getBigUint64 / setBigUint64
(function TestDataViewBigUint64() {
  var buf = new ArrayBuffer(16);
  var view = new DataView(buf);

  view.setBigUint64(0, 0n);
  assertEquals(0n, view.getBigUint64(0));

  view.setBigUint64(0, 1n);
  assertEquals(1n, view.getBigUint64(0));

  view.setBigUint64(0, 18446744073709551615n);
  assertEquals(18446744073709551615n, view.getBigUint64(0));

  // little-endian
  view.setBigUint64(0, 42n, true);
  assertEquals(42n, view.getBigUint64(0, true));
})();

// BigInt64Array from array
(function TestBigInt64ArrayFrom() {
  var a = new BigInt64Array([1n, 2n, 3n]);
  assertEquals(3, a.length);
  assertEquals(1n, a[0]);
  assertEquals(2n, a[1]);
  assertEquals(3n, a[2]);
})();

// BigUint64Array from array
(function TestBigUint64ArrayFrom() {
  var a = new BigUint64Array([1n, 2n, 3n]);
  assertEquals(3, a.length);
  assertEquals(1n, a[0]);
  assertEquals(2n, a[1]);
  assertEquals(3n, a[2]);
})();

"success";
