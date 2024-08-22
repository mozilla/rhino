/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

load("testsrc/assert.js");

(function TestSymbolSpecies() {
  var symbolSpeciesValue = Uint8ClampedArray[Symbol.species];
  assertEquals(Uint8ClampedArray, symbolSpeciesValue);
})();

(function TestSymbolToString() {
  var a = new Uint8ClampedArray(4);
  assertEquals('Uint8ClampedArray', a[Symbol.toStringTag]);
  assertEquals(false, a.hasOwnProperty(Symbol.toStringTag));
  assertEquals(false, Uint8ClampedArray.hasOwnProperty(Symbol.toStringTag));
  assertEquals(false, Uint8ClampedArray.prototype.hasOwnProperty(Symbol.toStringTag));
})();

"success";
