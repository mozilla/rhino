load("testsrc/assert.js");

var types = [Int8Array, Uint8Array, Int16Array, Uint16Array,
	Int32Array, Uint32Array, Uint8ClampedArray, Float32Array,
	Float64Array];


load("testsrc/assert.js");

var types = [Int8Array, Uint8Array, Int16Array, Uint16Array,
	Int32Array, Uint32Array, Uint8ClampedArray, Float32Array,
	Float64Array];

(function withNoArguments() {
	for (var t = 0; t < types.length; t++) {
		var type = types[t];
		var arr = new type([1, 2, 3]);
		var res = arr.with();
		assertEquals("1,2,3", arr.toString());
		assertFalse(arr === res);
		assertSame(Object.getPrototypeOf(arr), Object.getPrototypeOf(res));
		assertEquals("0,2,3", res.toString());
	}
})();

(function withIndex() {
	for (var t = 0; t < types.length; t++) {
		var type = types[t];
		var arr = new type([1, 2, 3]);
		var res = arr.with(1);
		assertEquals("1,0,3", res.toString());
	}
})();

(function withIndexValue() {
	for (var t = 0; t < types.length; t++) {
		var type = types[t];
		var arr = new type([1, 2, 3]);
		var res = arr.with(1, 4);
		assertEquals("1,4,3", res.toString());
	}
})();

(function withNegativeIndex() {
	for (var t = 0; t < types.length; t++) {
		var type = types[t];
		var arr = new type([1, 2, 3]);
		var res = arr.with(-2, 4);
		assertEquals("1,4,3", res.toString());
	}
})();

(function withIndexTooLarge() {
	for (var t = 0; t < types.length; t++) {
		var type = types[t];
		var arr = new type([1, 2, 3]);
		assertThrows(() => arr.with(3), RangeError);
	}
})();

(function withIgnoresSymbolSpecies() {
	var ta = new Int8Array([1, 2, 3]);
	ta.constructor = {
		[Symbol.species]: Uint8Array,
	};

	var res = ta.with(0, 4);
	assertEquals(Object.getPrototypeOf(res), Int8Array.prototype);
})();

"success";
