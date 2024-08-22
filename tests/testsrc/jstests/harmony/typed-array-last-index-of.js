load("testsrc/assert.js");

var types = [Int8Array, Uint8Array, Int16Array, Uint16Array,
             Int32Array, Uint32Array, Uint8ClampedArray, Float32Array,
             Float64Array];

for (var t = 0; t < types.length; t++) {
  var type = types[t];

  var empty = new type(0);
  assertEquals(-1, empty.lastIndexOf(0));
  
  var oneTwoThree = new type([1, 2, 3]);
  assertEquals(-1, oneTwoThree.lastIndexOf(0));
  assertEquals(0, oneTwoThree.lastIndexOf(1));

  assertEquals(-1, oneTwoThree.lastIndexOf(3, 1));

  assertEquals(1, oneTwoThree.lastIndexOf(2, -2));
  assertEquals(-1, oneTwoThree.lastIndexOf(2, -100));

  assertEquals(0, oneTwoThree.lastIndexOf(1.0));
  
  var repeated = new type([2, 1, 2]);
  assertEquals(2, repeated.lastIndexOf(2));
}

"success";