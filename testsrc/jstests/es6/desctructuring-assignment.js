// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

load("testsrc/assert.js");

(function TestSuccessDestructuring() {
  // var
  var [a, b] = [1, 2];
  assertEquals(1, a);
  assertEquals(2, b);

  // let
  let [c, d] = [3, 4];
  assertEquals(3, c);
  assertEquals(4, d);

  // const
  const [e, f] = [5, 6];
  assertEquals(5, e);
  assertEquals(6, f);
})();

(function TestDuplicateNameDestructuring() {
  // var
  var [a, a] = [1, 2];
  assertEquals(2, a);

  // let
  assertThrows("let [b, b] = [3, 4];", TypeError);

  // const
  assertThrows("const [c, c] = [5, 6];", TypeError);
})();

"success";
