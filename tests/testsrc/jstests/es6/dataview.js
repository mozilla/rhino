/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

load("testsrc/assert.js");

(function TestSymbolSpecies() {
  var symbolSpeciesValue = DataView[Symbol.species];
  assertEquals(undefined, symbolSpeciesValue);
})();

(function TestSymbolToString() {
  var ab = new ArrayBuffer(256);
  var d = new DataView(ab, 1, 255);;

  assertEquals('DataView', d[Symbol.toStringTag]);
  assertEquals(false, d.hasOwnProperty(Symbol.toStringTag));
  assertEquals(false, DataView.hasOwnProperty(Symbol.toStringTag));
  assertEquals(false, DataView.prototype.hasOwnProperty(Symbol.toStringTag));
})();

"success";
