/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

load("testsrc/assert.js");

(function TestSymbolSpecies() {
  var symbolSpeciesValue = BigUint64Array[Symbol.species];
  assertEquals(BigUint64Array, symbolSpeciesValue);
})();

(function TestSymbolToString() {
  var a = new BigUint64Array(4);
  assertEquals('BigUint64Array', a[Symbol.toStringTag]);
  assertEquals(false, a.hasOwnProperty(Symbol.toStringTag));

  assertEquals(undefined, BigUint64Array[Symbol.toStringTag]);
  assertEquals(false, BigUint64Array.hasOwnProperty(Symbol.toStringTag));
  assertEquals(false, BigUint64Array.prototype.hasOwnProperty(Symbol.toStringTag));
})();

(function TestPrototypeSymbolToString() {
  var a = new BigUint64Array(4).__proto__;
  assertEquals(undefined, a[Symbol.toStringTag]);
  assertEquals(false, a.hasOwnProperty(Symbol.toStringTag));
})();

"success";
