/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

load("testsrc/assert.js");

(function TestSymbolSpecies() {
  var symbolSpeciesValue = BigInt64Array[Symbol.species];
  assertEquals(BigInt64Array, symbolSpeciesValue);
})();

(function TestSymbolToString() {
  var a = new BigInt64Array(4);
  assertEquals('BigInt64Array', a[Symbol.toStringTag]);
  assertEquals(false, a.hasOwnProperty(Symbol.toStringTag));

  assertEquals(undefined, BigInt64Array[Symbol.toStringTag]);
  assertEquals(false, BigInt64Array.hasOwnProperty(Symbol.toStringTag));
  assertEquals(false, BigInt64Array.prototype.hasOwnProperty(Symbol.toStringTag));
})();

(function TestPrototypeSymbolToString() {
  var a = new BigInt64Array(4).__proto__;
  assertEquals(undefined, a[Symbol.toStringTag]);
  assertEquals(false, a.hasOwnProperty(Symbol.toStringTag));
})();

"success";
