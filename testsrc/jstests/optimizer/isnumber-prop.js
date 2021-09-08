// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

load("testsrc/assert.js");

// Optimizer.rewriteForNumberVariables
const one = 1;

assertEquals(2, (() => 1 + one)());
assertEquals(2, (() => one + 1)());
assertEquals(0, (() => 1 - one)());
assertEquals(0, (() => one - 1)());
assertEquals(1, (() => 1 * one)());
assertEquals(1, (() => one * 1)());
assertEquals(1, (() => 1 / one)());
assertEquals(1, (() => one / 1)());
assertEquals(0, (() => 1 % one)());
assertEquals(0, (() => one % 1)());
assertEquals(1, (() => 1 ** one)());
assertEquals(1, (() => one ** 1)());

assertEquals(0, (() => 1 ^ one)());
assertEquals(0, (() => one ^ 1)());
assertEquals(1, (() => 1 | one)());
assertEquals(1, (() => one | 1)());
assertEquals(1, (() => 1 & one)());
assertEquals(1, (() => one & 1)());
assertEquals(2, (() => 1 << one)());
assertEquals(2, (() => one << 1)());
assertEquals(0, (() => 1 >> one)());
assertEquals(0, (() => one >> 1)());

assertEquals(-2, (() => { var n = 1; return ~n; })());
assertEquals(1, (() => { var n = 1; return +n; })());
assertEquals(-1, (() => { var n = 1; return -n; })());

"success";
