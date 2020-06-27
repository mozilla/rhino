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
    assertEquals('☃★♲你', String.fromCodePoint(9731, 9733, 9842, 0x2F804))
})();

"success";