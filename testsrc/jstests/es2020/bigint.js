// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

load("testsrc/assert.js");

// optimizer test
const one = 1n;
assertEquals(2n, (() => 1n + one)());
assertEquals(2n, (() => one + 1n)());
assertEquals(0n, (() => 1n - one)());
assertEquals(0n, (() => one - 1n)());
assertEquals(1n, (() => 1n * one)());
assertEquals(1n, (() => one * 1n)());
assertEquals(1n, (() => 1n / one)());
assertEquals(1n, (() => one / 1n)());
assertEquals(0n, (() => 1n % one)());
assertEquals(0n, (() => one % 1n)());
assertEquals(1n, (() => 1n ** one)());
assertEquals(1n, (() => one ** 1n)());

assertEquals(0n, (() => 1n ^ one)());
assertEquals(0n, (() => one ^ 1n)());
assertEquals(1n, (() => 1n | one)());
assertEquals(1n, (() => one | 1n)());
assertEquals(1n, (() => 1n & one)());
assertEquals(1n, (() => one & 1n)());
assertEquals(2n, (() => 1n << one)());
assertEquals(2n, (() => one << 1n)());
assertEquals(0n, (() => 1n >> one)());
assertEquals(0n, (() => one >> 1n)());

assertEquals(-2n, (() => { var n = 1n; return ~n; })());
assertThrows(() => {var n = 1n; return +n; }, TypeError);
assertEquals(-1n, (() => { var n = 1n; return -n; })());

// Out of range check for BigInt
const MAX_INT = 2n ** 31n - 1n;
assertEquals(1n, 1n ** MAX_INT);
assertThrows(() => {
  2n ** MAX_INT;
}, RangeError);
assertThrows(() => {
  1n ** (MAX_INT + 1n);
}, RangeError);

assertEquals(0n, 0n << MAX_INT);
assertThrows(() => {
  1n << MAX_INT;
}, RangeError);
assertThrows(() => {
  1n << (MAX_INT + 1n);
}, RangeError);

assertEquals(0n, 1n >> MAX_INT);
assertThrows(() => {
  1n >> (MAX_INT + 1n);
}, RangeError);

"success";
