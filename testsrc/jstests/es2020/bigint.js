// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

load("testsrc/assert.js");

// optimizer test
const one = 1n;
assertEquals(2n, (() => 1n + one)());
assertEquals(0n, (() => 1n - one)());
assertEquals(1n, (() => 1n * one)());
assertEquals(1n, (() => 1n / one)());
assertEquals(0n, (() => 1n % one)());
assertEquals(1n, (() => 1n ** one)());
assertEquals(0n, (() => 1n ^ one)());
assertEquals(1n, (() => 1n | one)());
assertEquals(1n, (() => 1n & one)());
assertEquals(2n, (() => 1n << one)());
assertEquals(0n, (() => 1n >> one)());

"success";
