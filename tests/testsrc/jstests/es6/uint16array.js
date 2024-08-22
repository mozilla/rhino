/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

load("testsrc/assert.js");

(function TestSymbolSpecies() {
  var symbolSpeciesValue = Uint16Array[Symbol.species];
  assertEquals(Uint16Array, symbolSpeciesValue);
})();

(function TestSymbolToString() {
  var a = new Uint16Array(4);
  assertEquals('Uint16Array', a[Symbol.toStringTag]);
  assertEquals(false, a.hasOwnProperty(Symbol.toStringTag));
  assertEquals(false, Uint16Array.hasOwnProperty(Symbol.toStringTag));
  assertEquals(false, Uint16Array.prototype.hasOwnProperty(Symbol.toStringTag));
})();

"success";
