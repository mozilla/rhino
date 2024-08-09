/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

load("testsrc/assert.js");

// # Array.from
// ## sparse array
var a = Array.from({0: 123, length: 3});

assertEquals(a[0], 123);
assertTrue(1 in a);
assertEquals(a.map(_ => 'a'), ['a', 'a', 'a']);

var a = Array.from(Array(2));

assertTrue(0 in a);
assertEquals(a.map(_ => 'a'), ['a', 'a']);

(function TestSymbolSpecies() {
	var symbolSpeciesValue = Array[Symbol.species];
	assertEquals(Array, symbolSpeciesValue);
})();

(function TestSymbolUnScopables() {
	var symbolUnscopablesValue = Array[Symbol.unscopables];
	assertEquals(undefined, symbolUnscopablesValue);
})();

(function TestSymbolUnScopablesOnArray() {
	var symbolValuesToAssert = '{' +
								'"at":true,' +
								'"copyWithin":true,' +
								'"entries":true,' +
								'"fill":true,' +
								'"find":true,' +
								'"findIndex":true,' +
								'"findLast":true,' +
								'"findLastIndex":true,' +
								'"flat":true,' +
								'"flatMap":true,' +
								'"includes":true,' +
								'"keys":true,' +
								'"toReversed":true,' +
								'"toSorted":true,' +
								'"toSpliced":true,' +
								'"values":true' +
							'}';
	var symbolValues = Array.prototype[Symbol.unscopables];
	assertEquals(JSON.stringify(symbolValues), symbolValuesToAssert);
})();

"success";
