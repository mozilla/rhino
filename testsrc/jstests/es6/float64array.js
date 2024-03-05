/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

load("testsrc/assert.js");

(function TestSymbolSpecies() {
  var symbolSpeciesValue = Float64Array[Symbol.species];
  assertEquals(Float64Array, symbolSpeciesValue);
})();

(function TestSymbolToString() {
  var a = new Float64Array(4);
  assertEquals('Float64Array', a[Symbol.toStringTag]);
  assertEquals(false, a.hasOwnProperty(Symbol.toStringTag));
  assertEquals(false, Float64Array.hasOwnProperty(Symbol.toStringTag));
  assertEquals(false, Float64Array.prototype.hasOwnProperty(Symbol.toStringTag));
})();

"success";
