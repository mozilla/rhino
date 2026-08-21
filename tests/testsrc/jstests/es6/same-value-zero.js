// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

load("testsrc/assert.js");

(function TestIncludesNaN() {
  // Unlike indexOf, includes finds NaN elements.
  assertTrue([NaN].includes(NaN));
  assertTrue([1, NaN, 3].includes(0 / 0));
  assertTrue([Infinity - Infinity].includes(Math.sqrt(-1)));
  assertFalse([1, 2].includes(NaN));
  assertFalse([NaN].includes(0));
  assertFalse([NaN].includes("NaN"));
})();

(function TestIncludesSignedZero() {
  // Unlike indexOf (strict equality), includes treats +0 and -0 as equal.
  var arr = [0];
  assertTrue(arr.includes(-0));
  assertTrue(arr.includes(0 / -1));
  assertTrue(arr.includes(1 / -Infinity));
  var negArr = [-0];
  assertTrue(negArr.includes(0));
  assertTrue(negArr.includes(0.0));
  assertTrue(negArr.includes(-0.0));
  assertFalse([1].includes(0));
  assertFalse([0].includes(1));
})();

(function TestIncludesNumbers() {
  assertTrue([1].includes(1.0));
  assertTrue([1.0].includes(1));
  assertTrue([1.5].includes(3 / 2));
  assertTrue([0.1 + 0.2].includes(0.30000000000000004));
  assertTrue([Infinity].includes(1 / 0));
  assertTrue([-Infinity].includes(-1 / 0));
  assertTrue([Number.MIN_VALUE].includes(5e-324));
  assertTrue([Math.pow(2, 53)].includes(9007199254740992));
  // 2^53 + 1 rounds to 2^53.
  assertTrue([Math.pow(2, 53) + 1].includes(9007199254740992));
  assertFalse([Math.pow(2, 53) + 2].includes(9007199254740992));
  assertFalse([1].includes(2));
  assertFalse([1].includes(1.5));
  // Numbers never match values of other types.
  assertFalse([1].includes("1"));
  assertFalse(["1"].includes(1));
  assertFalse([1].includes(true));
  assertFalse([true].includes(1));
})();

(function TestIncludesOtherTypes() {
  assertTrue(["a", "b"].includes("a"));
  assertFalse(["ab"].includes("ba"));
  assertFalse(["1"].includes(1n));
  assertTrue([true].includes(true));
  assertTrue([false].includes(false));
  assertFalse([true].includes(false));
  assertTrue([null].includes(null));
  assertTrue([undefined].includes(undefined));
  assertFalse([null].includes(undefined));
  assertFalse([undefined].includes(null));
  // Holes are treated as undefined.
  var sparse = [1, , 3];
  assertTrue(sparse.includes(undefined));
  assertFalse(sparse.includes(null));
  var o = {};
  assertTrue([o].includes(o));
  assertFalse([{}].includes(o));
  var s = Symbol();
  assertTrue([s].includes(s));
  assertFalse([Symbol()].includes(s));
  // Boxed primitives are objects and only match by identity.
  var boxed = new Number(1);
  assertFalse([boxed].includes(1));
  assertFalse([1].includes(boxed));
  assertTrue([boxed].includes(boxed));
})();

(function TestIncludesBigInt() {
  assertTrue([1n].includes(1n));
  assertTrue([1n + 0n].includes(1n));
  assertTrue([2n ** 100n].includes(2n ** 99n * 2n));
  assertFalse([1n].includes(-1n));
  // BigInts never match numbers, even with the same value.
  assertFalse([1n].includes(1));
  assertFalse([1].includes(1n));
  assertFalse([0n].includes(0));
  assertFalse([0].includes(0n));
  assertFalse([9007199254740993n].includes(9007199254740992));
})();

(function TestIncludesStartIndex() {
  var arr = [1, 2, 3, 2];
  assertTrue(arr.includes(2));
  assertTrue(arr.includes(2, 1));
  assertFalse(arr.includes(3, 3));
  assertTrue(arr.includes(2, -3));
  assertTrue(arr.includes(2, -100));
  assertFalse(arr.includes(1, 1));
  assertFalse(arr.includes(1, 4));
  // The length-0 check happens before elements are examined.
  assertFalse([].includes(undefined));
})();

(function TestMapSignedZeroKeys() {
  var m = new Map();
  m.set(0, "plus");
  assertEquals("plus", m.get(-0));
  assertEquals("plus", m.get(0 / -1));
  m.set(-0, "minus");
  assertEquals(1, m.size);
  assertEquals("minus", m.get(0));
  m.delete(-0);
  assertEquals(0, m.size);
  assertFalse(m.has(0));
  var n = new Map();
  n.set(-0, "neg");
  assertEquals("neg", n.get(0.0));
  assertEquals("neg", n.get(0));
})();

(function TestMapOtherKeys() {
  var m = new Map();
  m.set(NaN, "nan");
  assertEquals("nan", m.get(NaN));
  assertEquals(1, m.size);
  m.set(0 / 0, "nan2");
  assertEquals(1, m.size);
  assertEquals("nan2", m.get(NaN));
  m.set(1, "num");
  m.set(1n, "bigint");
  assertEquals(3, m.size);
  assertEquals("num", m.get(1));
  assertEquals("bigint", m.get(1n));
  assertFalse(m.has(1.5));
  var o1 = {};
  var o2 = {};
  m.set(o1, "o1");
  m.set(o2, "o2");
  assertEquals(5, m.size);
  assertEquals("o1", m.get(o1));
  assertFalse(m.has({}));
  var s = Symbol("key");
  m.set(s, "sym");
  assertEquals("sym", m.get(s));
  assertFalse(m.has(Symbol("key")));
  m.delete(o1);
  assertEquals(5, m.size);
  assertFalse(m.has(o1));
})();

(function TestSetDistinctValues() {
  var set = new Set();
  set.add(0);
  set.add(-0);
  set.add(0 / -1);
  assertEquals(1, set.size);
  assertTrue(set.has(0));
  assertTrue(set.has(-0));
  set.add(NaN);
  set.add(0 / 0);
  assertEquals(2, set.size);
  assertTrue(set.has(NaN));
  set.add(1);
  set.add(1n);
  assertEquals(4, set.size);
  assertTrue(set.has(1));
  assertTrue(set.has(1n));
  var o = {};
  set.add(o);
  set.add(o);
  set.add({});
  assertEquals(6, set.size);
  set.delete(-0);
  assertEquals(5, set.size);
  assertFalse(set.has(0));
})();

(function TestTypedArrayIncludes() {
  var ia = new Int8Array([1, -1, 0]);
  assertTrue(ia.includes(0));
  assertTrue(ia.includes(-0));
  assertTrue(ia.includes(1));
  assertFalse(ia.includes(1.5));
  assertFalse(ia.includes("1"));
  var fa = new Float64Array([NaN, 0, -0]);
  assertTrue(fa.includes(NaN));
  assertTrue(fa.includes(0));
  assertTrue(fa.includes(-0));
  assertFalse(fa.includes(1));
  var sa = new Uint8ClampedArray([5, 6]);
  assertTrue(sa.includes(5));
  assertFalse(sa.includes(5n));
})();

"success";
