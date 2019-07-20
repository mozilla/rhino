// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

load("testsrc/assert.js");

(function TestRedefineLength() {
  var arr = [1,2,3,4];
  var res = "";
  try {
    Object.defineProperty(arr, "length", {
                get: function(){
                        return 11;
                }
      });
  } catch(e) {
    res = e.toString();
  }

  assertEquals("TypeError: Cannot redefine non-configurable property \"length\"", res);

})();

"success";