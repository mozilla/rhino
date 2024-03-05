/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

load("testsrc/assert.js");

(function TestSymbolSpecies() {
  var symbolSpeciesValue = Int16Array[Symbol.species];
  assertEquals(Int16Array, symbolSpeciesValue);
})();

(function TestSymbolToString() {
  var a = new Int16Array(4);
  assertEquals('Int16Array', a[Symbol.toStringTag]);
  assertEquals(false, a.hasOwnProperty(Symbol.toStringTag));
  assertEquals(false, Int16Array.hasOwnProperty(Symbol.toStringTag));
  assertEquals(false, Int16Array.prototype.hasOwnProperty(Symbol.toStringTag));
})();

"success";
