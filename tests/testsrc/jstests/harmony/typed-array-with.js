load("testsrc/assert.js");


var types = [Int8Array, Uint8Array, Int16Array, Uint16Array,
    Int32Array, Uint32Array, Uint8ClampedArray, Float32Array,
    Float64Array];

for (var t = 0; t < types.length; t++) {
    var type = types[t];

    var buffer = new ArrayBuffer(8 * type.BYTES_PER_ELEMENT);
    var arr = new type(buffer);

    var ta = arr.with(0, 11);
    assertEquals("0,0,0,0,0,0,0,0", arr.toString());
    assertEquals("11,0,0,0,0,0,0,0", ta.toString());

    ta = arr.with(7, 4);
    assertEquals("0,0,0,0,0,0,0,0", arr.toString());
    assertEquals("0,0,0,0,0,0,0,4", ta.toString());

    ta = arr.with(-1, 4);
    assertEquals("0,0,0,0,0,0,0,0", arr.toString());
    assertEquals("0,0,0,0,0,0,0,4", ta.toString());

    ta = arr.with(-8, 4);
    assertEquals("0,0,0,0,0,0,0,0", arr.toString());
    assertEquals("4,0,0,0,0,0,0,0", ta.toString());

    var msg = null;
    try { arr.with(-9, 42); } catch (e) { msg = e.toString();}
    assertEquals("RangeError: index -9 is out of bounds [-8..7]", msg);

    msg = null;
    try { arr.with(8, 42); } catch (e) { msg = e.toString();}
    assertEquals("RangeError: index 8 is out of bounds [-8..7]", msg);
}

"success";