load("testsrc/assert.js");

var types = [Int8Array, Uint8Array, Int16Array, Uint16Array,
             Int32Array, Uint32Array, Uint8ClampedArray, Float32Array,
             Float64Array];

Number.prototype.toLocaleString = function() {
  return this.toString() + 't';
}

for (var t = 0; t < types.length; t++) {
  var type = types[t];
  var arr = new type([1, 2, 3]);

  assertEquals(arr.toLocaleString(), "1t,2t,3t");
}

"success";