/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

load("testsrc/assert.js");

(function TestSymbolSpecies() {
  var symbolSpeciesValue = Int8Array[Symbol.species];
  assertEquals(Int8Array, symbolSpeciesValue);
})();

(function TestSymbolToString() {
  var a = new Int8Array(4);
  assertEquals('Int8Array', a[Symbol.toStringTag]);
  assertEquals(false, a.hasOwnProperty(Symbol.toStringTag));
  assertEquals(false, Int8Array.hasOwnProperty(Symbol.toStringTag));
  assertEquals(false, Int8Array.prototype.hasOwnProperty(Symbol.toStringTag));
})();

"success";
