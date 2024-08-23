load("testsrc/assert.js");

var types = [Int8Array, Uint8Array, Int16Array, Uint16Array,
	Int32Array, Uint32Array, Uint8ClampedArray, Float32Array,
	Float64Array];

(function toReversedBasic() {
	for (var t = 0; t < types.length; t++) {
		var type = types[t];
		var arr = new type([1, 2, 3]);
	
		var reversed = arr.toReversed();
		assertFalse(arr === reversed);
		assertSame(Object.getPrototypeOf(arr), Object.getPrototypeOf(reversed));
		assertEquals(3, reversed.length);
		assertEquals("3,2,1", reversed.toString());
	}
})();

(function toReversedIgnoresSymbolSpecies() {
	var ta = new Int8Array();
	ta.constructor = {
		[Symbol.species]: Uint8Array,
	};
	
	var reversed = ta.toReversed();
	assertEquals(Object.getPrototypeOf(reversed), Int8Array.prototype);
})();

"success";
