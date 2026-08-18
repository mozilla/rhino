load("testsrc/assert.js");

var types = [Int8Array, Uint8Array, Int16Array, Uint16Array,
	Int32Array, Uint32Array, Uint8ClampedArray, Float32Array,
	Float64Array];

var bigIntTypes = [BigInt64Array, BigUint64Array];

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

(function withBigIntValue() {
	for (var t = 0; t < bigIntTypes.length; t++) {
		var type = bigIntTypes[t];
		var arr = new type([1n, 2n, 3n]);
		var res = arr.with(1, 4n);
		assertEquals("1,4,3", res.toString());
		assertEquals("1,2,3", arr.toString());
	}
})();

(function withBigIntValueNegativeIndex() {
	for (var t = 0; t < bigIntTypes.length; t++) {
		var type = bigIntTypes[t];
		var arr = new type([1n, 2n, 3n]);
		var res = arr.with(-2, 4n);
		assertEquals("1,4,3", res.toString());
	}
})();

(function withBigIntValueEarlyCoercion() {
	for (var t = 0; t < bigIntTypes.length; t++) {
		var type = bigIntTypes[t];
		var arr = new type([0n, 1n, 2n]);
		var value = {
			valueOf: function() {
				arr[0] = 3n;
				return 4n;
			}
		};
		var res = arr.with(1, value);
		assertEquals("3,4,2", res.toString());
		assertEquals("3,1,2", arr.toString());
	}
})();

(function withNonBigIntTypedArrayRejectsBigIntValue() {
	for (var t = 0; t < types.length; t++) {
		var type = types[t];
		var arr = new type([1, 2, 3]);
		assertThrows(() => arr.with(1, 4n), TypeError);
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
