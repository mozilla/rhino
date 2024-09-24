load("testsrc/assert.js");

var types = [Int8Array, Uint8Array, Int16Array, Uint16Array,
	Int32Array, Uint32Array, Uint8ClampedArray, Float32Array,
	Float64Array];


load("testsrc/assert.js");

var signedTypes = [Int8Array, Int16Array, Int32Array, Float32Array, Float64Array];
var unsignedTypes = [Uint8Array, Uint16Array, Uint32Array, Uint8ClampedArray];

(function toSortedSigned() {
	for (var t = 0; t < signedTypes.length; t++) {
		var type = signedTypes[t];
		var arr = new type([3, -1, 2, -4, 5, -6, 0, 7]);
		var sorted = arr.toSorted();
		assertEquals("3,-1,2,-4,5,-6,0,7", arr.toString());
		assertFalse(arr === sorted);
		assertSame(Object.getPrototypeOf(arr), Object.getPrototypeOf(sorted));
		assertEquals("-6,-4,-1,0,2,3,5,7", sorted.toString());
		
		// Check stability
		arr = new type([-1, -3, -2, -4, -1, -3, 0, 3, 1]);
		sorted = arr.toSorted((a, b) => b.toString().length - a.toString().length)
		assertEquals("-1,-3,-2,-4,-1,-3,0,3,1", sorted.toString());
	}
})();

(function toSortedUnsigned() {
	for (var t = 0; t < unsignedTypes.length; t++) {
		var type = unsignedTypes[t];
		var arr = new type([3, 1, 2, 4, 5, 6, 0, 7]);
		var sorted = arr.toSorted();
		assertEquals("3,1,2,4,5,6,0,7", arr.toString());
		assertFalse(arr === sorted);
		assertSame(Object.getPrototypeOf(arr), Object.getPrototypeOf(sorted));
		assertEquals("0,1,2,3,4,5,6,7", sorted.toString());

		// Check stability
		sorted = arr.toSorted((a, b) => a % 2 - b % 2);
		assertEquals("2,4,6,0,3,1,5,7", sorted.toString());
	}
})();

(function toSortedIgnoresSymbolSpecies() {
	var ta = new Int8Array();
	ta.constructor = {
		[Symbol.species]: Uint8Array,
	};

	var reversed = ta.toSorted();
	assertEquals(Object.getPrototypeOf(reversed), Int8Array.prototype);
})();

"success";
