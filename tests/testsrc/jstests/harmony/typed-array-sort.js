load("testsrc/assert.js");

var signedTypes = [Int8Array, Int16Array, Int32Array, Float32Array, Float64Array];
var unsignedTypes = [Uint8Array, Uint16Array, Uint32Array, Uint8ClampedArray];

for (var t = 0; t < signedTypes.length; t++) {
    var type = signedTypes[t];
    var arr = new type([3, -1, 2, -4, 5, -6, 0, 7]);
    arr.sort();
    assertEquals("-6,-4,-1,0,2,3,5,7", arr.toString());
    
    arr = new type([-1, -3, -2, -4, -1, -3, 0, 3, 1]);
    arr.sort((a, b) => b.toString().length - a.toString().length)
    assertEquals("-1,-3,-2,-4,-1,-3,0,3,1", arr.toString());
}

for (var t = 0; t < unsignedTypes.length; t++) {
    var type = unsignedTypes[t];
    var arr = new type([3, 1, 2, 4, 5, 6, 0, 7]);
    arr.sort();
    assertEquals("0,1,2,3,4,5,6,7", arr.toString());
    
    arr.sort((a, b) => a % 2 - b % 2);
    assertEquals("0,2,4,6,1,3,5,7", arr.toString());
}
 
"success";