load("testsrc/assert.js");

var types = [Int8Array, Uint8Array, Int16Array, Uint16Array,
             Int32Array, Uint32Array, Uint8ClampedArray, Float32Array,
             Float64Array];

for (var t = 0; t < types.length; t++) {
  var type = types[t];

  var empty = new type(0);
  assertFalse(empty.includes(0));
  
  var oneTwoThree = new type([1, 2, 3]);
  assertFalse(oneTwoThree.includes(0));
  assertTrue(oneTwoThree.includes(1));
  
  assertFalse(oneTwoThree.includes(1, 2));
  
  assertTrue(oneTwoThree.includes(2, -2));
  assertTrue(oneTwoThree.includes(2, -100));
  
  assertTrue(oneTwoThree.includes(1.0));
}

"success";