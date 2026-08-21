// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

load("testsrc/assert.js");

(function TestObjectIsBasicNumbers() {
  assertTrue(Object.is(1, 1));
  assertTrue(Object.is(-1, -1));
  assertTrue(Object.is(1.5, 1.5));
  assertTrue(Object.is(-1.5, -1.5));
  assertTrue(Object.is(1.5, 3 / 2));
  assertTrue(Object.is(0.1 + 0.2, 0.30000000000000004));
  assertFalse(Object.is(1, 2));
  assertFalse(Object.is(1.5, 1.25));
  assertFalse(Object.is(1, -1));
})();

(function TestObjectIsMixedIntAndDouble() {
  // Integers and doubles with the same value must compare equal even when
  // they are internally boxed as different numeric types.
  assertTrue(Object.is(1, 1.0));
  assertTrue(Object.is(1.0, 1));
  assertTrue(Object.is(-1, -1.0));
  assertTrue(Object.is(-1.0, -1));
  assertTrue(Object.is(0, 0.0));
  assertTrue(Object.is(0.0, 0));
  assertTrue(Object.is(1152921504606846976, 1152921504606846976.0));
  assertTrue(Object.is(1152921504606846976.0, 1152921504606846976));
  assertFalse(Object.is(1, 1.5));
  assertFalse(Object.is(1.5, 1));
})();

(function TestObjectIsNaN() {
  assertTrue(Object.is(NaN, NaN));
  assertTrue(Object.is(0 / 0, NaN));
  assertTrue(Object.is(Math.sqrt(-1), NaN));
  assertTrue(Object.is(NaN * 1, NaN));
  assertTrue(Object.is(NaN + 1, NaN));
  assertTrue(Object.is(Infinity - Infinity, NaN));
  assertTrue(Object.is(0 * Infinity, NaN));
  assertFalse(Object.is(NaN, 0));
  assertFalse(Object.is(NaN, 1));
  assertFalse(Object.is(NaN, Infinity));
  assertFalse(Object.is(NaN, "NaN"));
})();

(function TestObjectIsSignedZero() {
  assertTrue(Object.is(0, 0));
  assertTrue(Object.is(-0, -0));
  assertFalse(Object.is(-0, 0));
  assertFalse(Object.is(0, -0));
  assertTrue(Object.is(0 / 1, 0));
  assertTrue(Object.is(0 / -1, -0));
  assertTrue(Object.is(0 * -1, -0));
  assertTrue(Object.is(1 / -Infinity, -0));
  assertTrue(Object.is(-1 / Infinity, -0));
  assertTrue(Object.is(-0 + 0, 0));
  assertTrue(Object.is(0 - 0, 0));
  // Mixed integer/double forms of zero keep their sign.
  assertFalse(Object.is(0, -0.0));
  assertFalse(Object.is(-0.0, 0));
  assertTrue(Object.is(-0, -0.0));
  assertTrue(Object.is(-0.0, -0));
})();

(function TestObjectIsInfinity() {
  assertTrue(Object.is(Infinity, Infinity));
  assertTrue(Object.is(-Infinity, -Infinity));
  assertTrue(Object.is(1 / 0, Infinity));
  assertTrue(Object.is(-1 / 0, -Infinity));
  assertFalse(Object.is(Infinity, -Infinity));
  assertFalse(Object.is(Infinity, Number.MAX_VALUE));
  assertFalse(Object.is(-Infinity, Number.MIN_VALUE));
  assertTrue(Object.is(1e308 * 10, Infinity));
})();

(function TestObjectIsExtremeValues() {
  assertTrue(Object.is(Number.MIN_VALUE, 5e-324));
  assertTrue(Object.is(5e-324, Number.MIN_VALUE));
  assertTrue(Object.is(Number.MAX_VALUE, 1.7976931348623157e308));
  assertTrue(Object.is(Math.pow(2, 53), 9007199254740992));
  // 2^53 + 1 is not representable; it rounds to 2^53.
  assertTrue(Object.is(Math.pow(2, 53) + 1, 9007199254740992));
  assertFalse(Object.is(Math.pow(2, 53) + 2, 9007199254740992));
})();

(function TestObjectIsOtherTypes() {
  assertTrue(Object.is(null, null));
  assertTrue(Object.is(undefined, undefined));
  assertFalse(Object.is(null, undefined));
  assertFalse(Object.is(undefined, null));
  assertTrue(Object.is("a", "a"));
  assertFalse(Object.is("a", "b"));
  assertFalse(Object.is("1", 1));
  assertFalse(Object.is(1, "1"));
  assertTrue(Object.is(true, true));
  assertTrue(Object.is(false, false));
  assertFalse(Object.is(true, false));
  assertFalse(Object.is(true, 1));
  assertFalse(Object.is(1, true));
  assertFalse(Object.is(true, "true"));
  var o = {};
  assertTrue(Object.is(o, o));
  assertFalse(Object.is(o, {}));
  var a = [];
  assertTrue(Object.is(a, a));
  assertFalse(Object.is(a, []));
  var r = /a/;
  assertTrue(Object.is(r, r));
  assertFalse(Object.is(r, /a/));
  var f = function () {};
  assertTrue(Object.is(f, f));
  assertFalse(Object.is(f, function () {}));
  assertFalse(Object.is(new Number(1), 1));
  assertFalse(Object.is(new Number(1), new Number(1)));
  assertFalse(Object.is(new String("a"), "a"));
  assertFalse(Object.is(new Boolean(true), true));
  assertFalse(Object.is({ valueOf: function () { return 1; } }, 1));
})();

(function TestObjectIsSymbols() {
  var s = Symbol();
  assertTrue(Object.is(s, s));
  assertFalse(Object.is(Symbol(), Symbol()));
  assertTrue(Object.is(Symbol.iterator, Symbol.iterator));
  assertFalse(Object.is(Symbol.iterator, Symbol()));
  assertFalse(Object.is(s, "a"));
  assertFalse(Object.is(s, null));
})();

(function TestObjectIsBigInts() {
  assertTrue(Object.is(1n, 1n));
  assertTrue(Object.is(-1n, -1n));
  assertTrue(Object.is(0n, 0n));
  assertTrue(Object.is(-0n, 0n));
  assertFalse(Object.is(1n, -1n));
  assertTrue(Object.is(1n + 0n, 1n));
  assertTrue(Object.is(2n ** 100n, 2n ** 100n));
  assertTrue(Object.is(2n ** 100n, 2n ** 99n * 2n));
  assertFalse(Object.is(2n ** 100n, 2n ** 99n));
  assertTrue(
      Object.is(123456789012345678901234567890n, 123456789012345678901234567890n));
  assertFalse(
      Object.is(123456789012345678901234567890n, 123456789012345678901234567891n));
  // BigInts never compare equal to numbers, even with the same value.
  assertFalse(Object.is(1n, 1));
  assertFalse(Object.is(1, 1n));
  assertFalse(Object.is(1n, 1.0));
  assertFalse(Object.is(1.0, 1n));
  assertFalse(Object.is(0n, 0));
  assertFalse(Object.is(0n, -0));
  assertFalse(Object.is(9007199254740993n, 9007199254740992));
  assertFalse(Object.is(9007199254740993n, 9007199254740993));
  assertFalse(Object.is(BigInt("18446744073709551616"), 18446744073709551616));
  // BigInts differ from every other primitive type as well.
  assertFalse(Object.is(1n, "1"));
  assertFalse(Object.is(1n, true));
  assertFalse(Object.is(1n, null));
  assertFalse(Object.is(1n, undefined));
  // Boxed BigInt objects are objects, so they only match by identity.
  var bn = Object(1n);
  assertTrue(Object.is(bn, bn));
  assertFalse(Object.is(bn, Object(1n)));
  assertFalse(Object.is(1n, bn));
  assertFalse(Object.is(bn, 1n));
  assertFalse(Object.is(bn, 2n));
})();

"success";
