/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

load("testsrc/assert.js");

// Math.sumPrecise sums as if with unlimited precision and rounds once.

// Function shape
assertEquals("sumPrecise", Math.sumPrecise.name);
assertEquals(1, Math.sumPrecise.length);
var desc = Object.getOwnPropertyDescriptor(Math, "sumPrecise");
assertTrue(desc.writable);
assertFalse(desc.enumerable);
assertTrue(desc.configurable);
assertThrows(function() { new Math.sumPrecise([]); }, TypeError);

// Exact where naive addition is not
assertEquals(0.6, Math.sumPrecise([0.1, 0.2, 0.3]));
assertEquals(0.1, Math.sumPrecise([1e20, 0.1, -1e20]));
assertEquals(2, Math.sumPrecise([1, 1e100, 1, -1e100]));
assertEquals(1e-300, Math.sumPrecise([1e300, 1e-300, -1e300]));
assertEquals(3, Math.sumPrecise([1, 2]));
assertEquals(-1, Math.sumPrecise([1, -2]));

// Signed zeros: an empty sum and a sum of only -0 are -0, any +0 makes it +0
assertEquals(-0, Math.sumPrecise([]));
assertEquals(-0, Math.sumPrecise([-0]));
assertEquals(-0, Math.sumPrecise([-0, -0]));
assertEquals(0, Math.sumPrecise([0]));
assertEquals(0, Math.sumPrecise([-0, 0]));
assertEquals(0, Math.sumPrecise([0, -0]));
assertEquals(0, Math.sumPrecise([1, -1]));
assertEquals(0, Math.sumPrecise([-0, 1, -1]));

// Infinities and NaN
assertEquals(Infinity, Math.sumPrecise([Infinity]));
assertEquals(Infinity, Math.sumPrecise([1, Infinity, -1]));
assertEquals(-Infinity, Math.sumPrecise([-Infinity, 1e308]));
assertEquals(NaN, Math.sumPrecise([Infinity, -Infinity]));
assertEquals(NaN, Math.sumPrecise([-Infinity, Infinity]));
assertEquals(NaN, Math.sumPrecise([NaN]));
assertEquals(NaN, Math.sumPrecise([1, NaN, Infinity]));

// Overflow: the exact sum decides, not the running total
assertEquals(Infinity, Math.sumPrecise([Number.MAX_VALUE, Number.MAX_VALUE]));
assertEquals(-Infinity, Math.sumPrecise([-Number.MAX_VALUE, -Number.MAX_VALUE]));
assertEquals(Number.MAX_VALUE,
    Math.sumPrecise([Number.MAX_VALUE, Number.MAX_VALUE, -Number.MAX_VALUE]));
assertEquals(-Number.MAX_VALUE,
    Math.sumPrecise([-Number.MAX_VALUE, -Number.MAX_VALUE, Number.MAX_VALUE]));
assertEquals(Number.MAX_VALUE, Math.sumPrecise([Number.MAX_VALUE, 1e292, -1e292]));

// Any iterable, consumed in order
assertEquals(6, Math.sumPrecise(new Set([1, 2, 3])));
assertEquals(6, Math.sumPrecise(new Map([[1, 2], [3, 4]]).values()));
function* gen() { yield 0.1; yield 0.2; yield 0.3; }
assertEquals(0.6, Math.sumPrecise(gen()));
var order = [];
var iterable = {
  [Symbol.iterator]() {
    var i = 0;
    return {
      next() { order.push(i); return i < 3 ? { value: i++, done: false } : { done: true }; }
    };
  }
};
assertEquals(3, Math.sumPrecise(iterable));
assertEquals("0,1,2,3", order.join());

// A non-Number is a TypeError and nothing is coerced
assertThrows(function() { Math.sumPrecise([1, "2"]); }, TypeError);
assertThrows(function() { Math.sumPrecise(["1"]); }, TypeError);
assertThrows(function() { Math.sumPrecise([true]); }, TypeError);
assertThrows(function() { Math.sumPrecise([null]); }, TypeError);
assertThrows(function() { Math.sumPrecise([undefined]); }, TypeError);
assertThrows(function() { Math.sumPrecise([1n]); }, TypeError);
assertThrows(function() { Math.sumPrecise([new Number(1)]); }, TypeError);
assertThrows(function() { Math.sumPrecise([NaN, "x"]); }, TypeError);
assertThrows(function() { Math.sumPrecise("12"); }, TypeError);

// The argument must be iterable
assertThrows(function() { Math.sumPrecise(); }, TypeError);
assertThrows(function() { Math.sumPrecise(undefined); }, TypeError);
assertThrows(function() { Math.sumPrecise(null); }, TypeError);
assertThrows(function() { Math.sumPrecise(1); }, TypeError);
assertThrows(function() { Math.sumPrecise({}); }, TypeError);
assertThrows(function() { Math.sumPrecise(1, 2); }, TypeError);

// The iterator is closed when a value is rejected
var closed = 0;
var closing = {
  [Symbol.iterator]() {
    return {
      next() { return { value: "x", done: false }; },
      return() { closed++; return { done: true }; }
    };
  }
};
assertThrows(function() { Math.sumPrecise(closing); }, TypeError);
assertEquals(1, closed);

"success";
