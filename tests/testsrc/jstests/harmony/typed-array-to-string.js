load("testsrc/assert.js");

var types = [Int8Array, Uint8Array, Int16Array, Uint16Array,
             Int32Array, Uint32Array, Uint8ClampedArray, Float32Array,
             Float64Array];

for (var t = 0; t < types.length; t++) {
  var type = types[t];

  var empty = new type(0);
  assertEquals("", empty.toString());

  var one = new type([7]);
  assertEquals("7", one.toString());

  var two = new type([7,13]);
  assertEquals("7,13", two.toString());

  var many = new type([7,13,21,17,0,1,11]);
  assertEquals("7,13,21,17,0,1,11", many.toString());
}

"success";