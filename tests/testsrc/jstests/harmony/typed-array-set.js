load("testsrc/assert.js");


var types = [Int8Array, Uint8Array, Int16Array, Uint16Array,
    Int32Array, Uint32Array, Uint8ClampedArray, Float32Array,
    Float64Array];

for (var t = 0; t < types.length; t++) {
    var type = types[t];

    var buffer = new ArrayBuffer(8 * type.BYTES_PER_ELEMENT);
    var arr = new type(buffer);

    arr.set([1, 2, 3], 3);
    assertEquals("0,0,0,1,2,3,0,0", arr.toString());

    arr.set([7], 0);
    assertEquals("7,0,0,1,2,3,0,0", arr.toString());

    arr.set([8, 9]);
    assertEquals("8,9,0,1,2,3,0,0", arr.toString());

    var msg = null;
    try { arr.set([0], -1); } catch (e) { msg = e.toString();}
    assertEquals("RangeError: offset -1 out of range", msg);

    arr.set([], 8);
    assertEquals("8,9,0,1,2,3,0,0", arr.toString());

    msg = null;
    try { arr.set([], 9); } catch (e) { msg = e.toString();}
    assertEquals("RangeError: offset 9 out of range", msg);

    arr.set([1], 7);
    assertEquals("8,9,0,1,2,3,0,1", arr.toString());

    msg = null;
    try { arr.set([1], 8); } catch (e) { msg = e.toString();}
    assertEquals("RangeError: source array is too long", msg);

    arr.set([0, 1, 2, 3, 4, 5, 6, 7]);
    assertEquals("0,1,2,3,4,5,6,7", arr.toString());

    msg = null;
    try { arr.set([0, 1, 2, 3, 4, 5, 6, 7, 8]); } catch (e) { msg = e.toString();}
    assertEquals("RangeError: source array is too long", msg);
}

for (var t = 0; t < types.length; t++) {
    var type = types[t];

    var buffer = new ArrayBuffer(8 * type.BYTES_PER_ELEMENT);
    var arr = new type(buffer);

    arr.set(new type([1, 2, 3]), 3);
    assertEquals("0,0,0,1,2,3,0,0", arr.toString());

    arr.set(new type([7]), 0);
    assertEquals("7,0,0,1,2,3,0,0", arr.toString());

    arr.set(new type([8, 9]));
    assertEquals("8,9,0,1,2,3,0,0", arr.toString());

    var msg = null;
    try { arr.set(new type([0]), -1); } catch (e) { msg = e.toString();}
    assertEquals("RangeError: offset -1 out of range", msg);

    arr.set(new type([]), 8);
    assertEquals("8,9,0,1,2,3,0,0", arr.toString());

    msg = null;
    try { arr.set(new type([]), 9); } catch (e) { msg = e.toString();}
    assertEquals("RangeError: offset 9 out of range", msg);

    arr.set(new type([1]), 7);
    assertEquals("8,9,0,1,2,3,0,1", arr.toString());

    msg = null;
    try { arr.set(new type([1]), 8); } catch (e) { msg = e.toString();}
    assertEquals("RangeError: source array is too long", msg);

    arr.set(new type([0, 1, 2, 3, 4, 5, 6, 7]));
    assertEquals("0,1,2,3,4,5,6,7", arr.toString());

    msg = null;
    try { arr.set(new type([0, 1, 2, 3, 4, 5, 6, 7, 8])); } catch (e) { msg = e.toString();}
    assertEquals("RangeError: source array is too long", msg);}

"success";