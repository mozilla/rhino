// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

load("testsrc/assert.js");

(function TestRedefineLength() {
  var res = "";
  try {
    Number.parseFloat('0.004').toFixed(-2);
  } catch(e) {
    res = e.toString();
  }

  assertEquals("RangeError: Precision -2 out of range.", res);

})();

"success";