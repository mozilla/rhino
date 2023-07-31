/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

load("testsrc/assert.js");

(function TestSymbolSpecies() {
  var symbolSpeciesValue = Uint32Array[Symbol.species];
  assertEquals(Uint32Array, symbolSpeciesValue);
})();

(function TestSymbolToString() {
  var a = new Uint32Array(4);
  assertEquals('Uint32Array', a[Symbol.toStringTag]);
  assertEquals(false, a.hasOwnProperty(Symbol.toStringTag));
  assertEquals(false, Uint32Array.hasOwnProperty(Symbol.toStringTag));
  assertEquals(false, Uint32Array.prototype.hasOwnProperty(Symbol.toStringTag));
})();

"success";
