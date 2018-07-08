load("testsrc/assert.js");

var types = [Int8Array, Uint8Array, Int16Array, Uint16Array,
             Int32Array, Uint32Array, Uint8ClampedArray, Float32Array,
             Float64Array];

var type;

function foo() {
  return new type(arguments);
}

for (var t = 0; t < types.length; t++) {
  type = types[t];

  var one = new foo([7]);
  assertEquals("7", one.toString());

  var two = new foo(7, 13);
  assertEquals("7,13", two.toString());

  var many = new foo(7,13,21,17,0,1,11);
  assertEquals("7,13,21,17,0,1,11", many.toString());
}

"success";