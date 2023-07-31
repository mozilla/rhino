// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

load("testsrc/assert.js");

(function TestIsEnumerable() {
  var obj = new Object("z");

  assertTrue(obj.propertyIsEnumerable('0'));
})();

(function TestReplaceNotUsedArguments() {
  assertEquals("axdefg", "abcdefg".replace("bc", "x", "not used"));
})();

(function TestMatchNotUsedArguments() {
  assertNull("abcdefg".match("AB", "not used"));
})();

(function TestSearchNotUsedArguments() {
  assertEquals(2, "abcdEfg".search("cd", "not used"));
})();

(function TestFromCodePoint() {
    // Text: ☃★♲你
    assertEquals('\u2603\u2605\u2672\uD87E\uDC04', String.fromCodePoint(0x2603, 0x2605, 0x2672, 0x2F804))
})();

(function TestTrimStart() {
    assertEquals("abc ", " abc ".trimStart())
    assertEquals("abc ", " abc ".trimLeft())
})();

(function TestTrimEnd() {
    assertEquals(" abc", " abc ".trimEnd())
    assertEquals(" abc", " abc ".trimRight())
})();

(function TestRawTooLarge() {
  assertThrows(()=>String.raw({raw: {length: Math.pow(2, 31) + 1}}), RangeError);
})();

(function TestRawPrototypeGet() {
  var _raw = ["1",,"3"];
  var raw = Object.create(_raw);
  raw[2] = "2*";
  raw.length = 4;
  assertEquals("1.undefined^2*undefined", String.raw(Object.create({raw}), ".", "^"));
})();

"success";
