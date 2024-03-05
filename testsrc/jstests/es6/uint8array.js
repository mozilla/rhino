/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

load("testsrc/assert.js");

(function TestSymbolSpecies() {
  var symbolSpeciesValue = Uint8Array[Symbol.species];
  assertEquals(Uint8Array, symbolSpeciesValue);
})();

(function TestSymbolToString() {
  var a = new Uint8Array(4);
  assertEquals('Uint8Array', a[Symbol.toStringTag]);
  assertEquals(false, a.hasOwnProperty(Symbol.toStringTag));
  assertEquals(false, Uint8Array.hasOwnProperty(Symbol.toStringTag));
  assertEquals(false, Uint8Array.prototype.hasOwnProperty(Symbol.toStringTag));
})();

"success";
