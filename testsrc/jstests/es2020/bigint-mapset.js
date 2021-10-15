// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

// https://github.com/mozilla/rhino/issues/1023

load("testsrc/assert.js");

var lo53 = 9007199254740991 // 2^53 - 1
var big53 = BigInt("9007199254740991")
var lo54 = 9007199254740992
var lo54a = 9007199254740993
var big54 = BigInt("9007199254740992")
var big = BigInt("100000000000000000000000000000000000000000000000000000000000000000000000000000000001")
var d = 1e+83
var m = new Map()
var s = new Set()

m.set(lo53, 'lo53 key')
m.set(big53, 'big53 key')
m.set(lo54, 'lo54 key')
m.set(lo54a, 'lo54a key')
m.set(big54, 'big54 key')
m.set(big, 'bigint key')
m.set(d, 'double key')

assertEquals('lo53 key', m.get(lo53));
assertEquals('big53 key', m.get(big53));
assertEquals('lo54a key', m.get(lo54));
assertEquals('lo54a key', m.get(lo54a));
assertEquals('big54 key', m.get(big54));
assertEquals('bigint key', m.get(big));
assertEquals('double key', m.get(d));

s.add(lo53)
s.add(lo54)

assertTrue(s.has(lo53));
assertTrue(s.has(lo54));
assertTrue(s.has(lo54a));
assertFalse(s.has(big53));
assertFalse(s.has(big54));

s.add(big53)
s.add(big54)

assertTrue(s.has(lo53));
assertTrue(s.has(lo54));
assertTrue(s.has(big53));
assertTrue(s.has(big54));
assertFalse(s.has(big));
assertFalse(s.has(1));

"success";
