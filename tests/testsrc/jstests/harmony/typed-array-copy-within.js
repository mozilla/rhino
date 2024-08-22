load("testsrc/assert.js");


var types = [Int8Array, Uint8Array, Int16Array, Uint16Array,
    Int32Array, Uint32Array, Uint8ClampedArray, Float32Array,
    Float64Array];

for (var t = 0; t < types.length; t++) {
    var type = types[t];
    
    var arr = new type([0, 1, 2, 3, 4, 5, 6]);
    arr.copyWithin(4, 1);
    assertEquals("0,1,2,3,1,2,3", arr.toString());

    arr = new type([0, 1, 2, 3, 4, 5, 6]);
    arr.copyWithin(-2, -5);
    assertEquals("0,1,2,3,4,2,3", arr.toString());

    arr = new type([0, 1, 2, 3, 4, 5, 6]);
    arr.copyWithin(3, 1, 4);
    assertEquals("0,1,2,1,2,3,6", arr.toString());
}
 
"success";