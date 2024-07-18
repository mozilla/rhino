load("testsrc/assert.js");


var types = [Int8Array, Uint8Array, Int16Array, Uint16Array,
    Int32Array, Uint32Array, Uint8ClampedArray, Float32Array,
    Float64Array];

for (var t = 0; t < types.length; t++) {
    var type = types[t];
       
    var empty = new type();
    var emptyFiltered = empty.filter(() => false);
    assertTrue(emptyFiltered instanceof type);
    assertEquals(0, emptyFiltered.length);
    
    var arr = new type([1, 2, 3]);
    var arrFiltered = arr.filter(n => n > 1);
    assertTrue(arrFiltered instanceof type);
    assertEquals(2, arrFiltered.length);
    assertEquals("2,3", arrFiltered.toString());

    // Check that the constructor property is used correctly
    arr.constructor = Int16Array;
    arrFiltered = arr.filter(n => n > 1);
    assertTrue(arrFiltered instanceof Int16Array);
    assertEquals(2, arrFiltered.length);
    assertEquals("2,3", arrFiltered.toString());

    arr.constructor = 42;
    try {
        arr.filter(n => n > 1);
        assertTrue(false);
    } catch (e) {
        assertTrue(e instanceof TypeError);
    }

    arr.constructor = {};
    arr.constructor[Symbol.species] = Array;
    try {
        arr.filter(n => n > 1);
        assertTrue(false);
    } catch (e) {
        assertTrue(e instanceof TypeError);
    }
}

"success";