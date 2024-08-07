load("testsrc/assert.js");


var types = [Int8Array, Uint8Array, Int16Array, Uint16Array,
    Int32Array, Uint32Array, Uint8ClampedArray, Float32Array,
    Float64Array];

for (var t = 0; t < types.length; t++) {
    var type = types[t];
       
    var arr = new type([1, 2, 3]);
    assertEquals(2, arr.findLast(n => n % 2 === 0));
    assertEquals(undefined, arr.findLast(n => n < 0));
}
 
"success";