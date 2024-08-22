load("testsrc/assert.js");

var types = [Int8Array, Uint8Array, Int16Array, Uint16Array,
             Int32Array, Uint32Array, Uint8ClampedArray, Float32Array,
             Float64Array];
var intTypes = types.slice(0, types.indexOf(Float32Array));
var floatTypes = types.slice(types.indexOf(Float32Array));

for (var t = 0; t < types.length; t++) {
    var type = types[t];
    var arr = new type([1, 2, 3]);

    arr.fill(4);
    assertEquals("4,4,4", arr.toString());

    arr.fill(5, 1);
    assertEquals("4,5,5", arr.toString());

    arr.fill(6, -2);
    assertEquals("4,6,6", arr.toString());

    arr.fill(7, 0, 1);
    assertEquals("7,6,6", arr.toString());

    arr.fill(8, 0, -1);
    assertEquals("8,8,6", arr.toString());
}
 
for (var t = 0; t < intTypes.length; t++) {
  var type = intTypes[t];
  var arr = new type([1, 2, 3]);
  arr.fill();
  assertEquals("0,0,0", arr.toString());
}

for (var t = 0; t < floatTypes.length; t++) {
  var type = floatTypes[t];
  var arr = new type([1, 2, 3]);
  arr.fill();
  assertEquals("NaN,NaN,NaN", arr.toString());
}

"success";