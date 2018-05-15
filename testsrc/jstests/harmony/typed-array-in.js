load("testsrc/assert.js");

var types = [Int8Array, Uint8Array, Int16Array, Uint16Array,
             Int32Array, Uint32Array, Uint8ClampedArray, Float32Array,
             Float64Array];

for (var t = 0; t < types.length; t++) {
  var type = types[t];

  var empty = new type(0);
  assertFalse(0 in empty);
  assertFalse(1 in empty);

  var one = new type(1);
  one[0] = 1;
  assertTrue(0 in one);
  assertFalse(1 in one);

  var two = new type(2);
  assertTrue(0 in two);
  assertTrue(1 in two);
  assertFalse(2 in two);

  two[1] = 2;
  assertFalse(2 in two);
}

"success";