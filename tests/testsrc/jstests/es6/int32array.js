/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

load("testsrc/assert.js");

(function TestSymbolSpecies() {
  var symbolSpeciesValue = Int32Array[Symbol.species];
  assertEquals(Int32Array, symbolSpeciesValue);
})();

(function TestSymbolToString() {
  var a = new Int32Array(4);
  assertEquals('Int32Array', a[Symbol.toStringTag]);
  assertEquals(false, a.hasOwnProperty(Symbol.toStringTag));
  assertEquals(false, Int32Array.hasOwnProperty(Symbol.toStringTag));
  assertEquals(false, Int32Array.prototype.hasOwnProperty(Symbol.toStringTag));
})();

"success";
