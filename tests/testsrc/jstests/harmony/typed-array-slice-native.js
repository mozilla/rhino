load("testsrc/assert.js");

var types = [Int8Array, Uint8Array, Int16Array, Uint16Array,
             Int32Array, Uint32Array, Uint8ClampedArray, Float32Array,
             Float64Array];

for (var t = 0; t < types.length; t++) {
  var type = types[t];

  var oneTwoThreeFour = new type([1, 2, 3, 4]);
  
  var slice = oneTwoThreeFour.slice();
  assertTrue(slice instanceof type);
  assertEquals(4, slice.length);
  assertEquals("1,2,3,4", slice.toString());
  
  slice = oneTwoThreeFour.slice(1);
  assertEquals(3, slice.length);
  assertEquals("2,3,4", slice.toString());

  slice = oneTwoThreeFour.slice(1, 3);
  assertEquals(2, slice.length);
  assertEquals("2,3", slice.toString());

  slice = oneTwoThreeFour.slice(42, 0);
  assertEquals(0, slice.length);
  assertEquals("", slice.toString());

  slice = oneTwoThreeFour.slice(0, 42);
  assertEquals(4, slice.length);
  assertEquals("1,2,3,4", slice.toString());

  // Check that the constructor property is used correctly
  oneTwoThreeFour.constructor = 42;
  try {
    slice = oneTwoThreeFour.slice(1, 3);
    assertTrue(false);
  } catch (e) {
    assertTrue(e instanceof TypeError);
  }

  oneTwoThreeFour.constructor = {};
  oneTwoThreeFour.constructor[Symbol.species] = Array;
  try {
    slice = oneTwoThreeFour.slice(1, 3);
    assertTrue(false);
  } catch (e) {
    assertTrue(e instanceof TypeError);
  }
}

"success";