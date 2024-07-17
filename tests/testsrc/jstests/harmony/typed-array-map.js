load("testsrc/assert.js");


var types = [Int8Array, Uint8Array, Int16Array, Uint16Array,
    Int32Array, Uint32Array, Uint8ClampedArray, Float32Array,
    Float64Array];

for (var t = 0; t < types.length; t++) {
    var type = types[t];
       
    var empty = new type();
    var emptyFiltered = empty.map(n => n + 1);
    assertTrue(emptyFiltered instanceof type);
    assertEquals(0, emptyFiltered.length);
    
    var arr = new type([1, 2, 3]);
    var arrFiltered = arr.map(n => n + 1);
    assertTrue(arrFiltered instanceof type);
    assertEquals(3, arrFiltered.length);
    assertEquals("2,3,4", arrFiltered.toString());
}
 
"success";