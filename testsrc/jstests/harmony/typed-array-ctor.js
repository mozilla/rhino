load("testsrc/assert.js");

  // Int8Array
  var empty = new Int8Array();
  assertEquals("", empty.toString());

  empty = new Int8Array(0);
  assertEquals("", empty.toString());

  var init = new Int8Array(2);
  assertEquals("0,0", init.toString());

  var msg = null;
  try { new Int8Array(-2); } catch (e) { msg = e.toString();}
  assertEquals("RangeError: Negative array length -2.0", msg);

  msg = null;
  try { new Int8Array(-268435457); } catch (e) { msg = e.toString();}
  assertEquals("RangeError: Negative array length -2.68435457E8", msg);

  var one = new Int8Array([7]);
  assertEquals("7", one.toString());

  var two = new Int8Array([7, 13]);
  assertEquals("7,13", two.toString());

  var many = new Int8Array([7,13,21,17,0,1,11]);
  assertEquals("7,13,21,17,0,1,11", many.toString());

  var special = new Int8Array([undefined, null, Number.NaN, Number.POSITIVE_INFINITY, Number.NEGATIVE_INFINITY, 4]);
  assertEquals("0,0,0,0,0,4", special.toString());

  special = new Int8Array(6);
  special[0] = undefined;
  special[1] = null;
  special[2] = Number.NaN;
  special[3] = Number.POSITIVE_INFINITY;
  special[4] = Number.NEGATIVE_INFINITY;
  special[5] = 4;
  assertEquals("0,0,0,0,0,4", special.toString());

  var udefArr = new Int8Array(undefined);
  assertEquals(0, udefArr.length);

  var nullArr = new Int8Array(null);
  assertEquals(0, nullArr.length);

  // Uint8Array
  empty = new Uint8Array();
  assertEquals("", empty.toString());

  empty = new Uint8Array(0);
  assertEquals("", empty.toString());

  init = new Uint8Array(2);
  assertEquals("0,0", init.toString());

  msg = null;
  try { new Uint8Array(-2); } catch (e) { msg = e.toString();}
  assertEquals("RangeError: Negative array length -2.0", msg);

  msg = null;
  try { new Uint8Array(-268435457); } catch (e) { msg = e.toString();}
  assertEquals("RangeError: Negative array length -2.68435457E8", msg);

  one = new Uint8Array([7]);
  assertEquals("7", one.toString());

  two = new Uint8Array([7, 13]);
  assertEquals("7,13", two.toString());

  many = new Uint8Array([7,13,21,17,0,1,11]);
  assertEquals("7,13,21,17,0,1,11", many.toString());

  special = new Uint8Array([undefined, null, Number.NaN, Number.POSITIVE_INFINITY, Number.NEGATIVE_INFINITY, 4]);
  assertEquals("0,0,0,0,0,4", special.toString());

  special = new Uint8Array(6);
  special[0] = undefined;
  special[1] = null;
  special[2] = Number.NaN;
  special[3] = Number.POSITIVE_INFINITY;
  special[4] = Number.NEGATIVE_INFINITY;
  special[5] = 4;
  assertEquals("0,0,0,0,0,4", special.toString());

  udefArr = new Uint8Array(undefined);
  assertEquals(0, udefArr.length);

  nullArr = new Uint8Array(null);
  assertEquals(0, nullArr.length);


  // Int16Array
  empty = new Int16Array();
  assertEquals("", empty.toString());

  empty = new Int16Array(0);
  assertEquals("", empty.toString());

  init = new Int16Array(2);
  assertEquals("0,0", init.toString());

  msg = null;
  try { new Int16Array(-2); } catch (e) { msg = e.toString();}
  assertEquals("RangeError: Negative array length -4.0", msg);

  msg = null;
  try { new Int16Array(-268435457); } catch (e) { msg = e.toString();}
  assertEquals("RangeError: Negative array length -5.36870914E8", msg);

  one = new Int16Array([7]);
  assertEquals("7", one.toString());

  two = new Int16Array([7, 13]);
  assertEquals("7,13", two.toString());

  many = new Int16Array([7,13,21,17,0,1,11]);
  assertEquals("7,13,21,17,0,1,11", many.toString());

  special = new Int16Array([undefined, null, Number.NaN, Number.POSITIVE_INFINITY, Number.NEGATIVE_INFINITY, 4]);
  assertEquals("0,0,0,0,0,4", special.toString());

  special = new Int16Array(6);
  special[0] = undefined;
  special[1] = null;
  special[2] = Number.NaN;
  special[3] = Number.POSITIVE_INFINITY;
  special[4] = Number.NEGATIVE_INFINITY;
  special[5] = 4;
  assertEquals("0,0,0,0,0,4", special.toString());

  udefArr = new Int16Array(undefined);
  assertEquals(0, udefArr.length);

  nullArr = new Int16Array(null);
  assertEquals(0, nullArr.length);


  // Uint16Array
  empty = new Uint16Array();
  assertEquals("", empty.toString());

  empty = new Uint16Array(0);
  assertEquals("", empty.toString());

  init = new Uint16Array(2);
  assertEquals("0,0", init.toString());

  msg = null;
  try { new Uint16Array(-2); } catch (e) { msg = e.toString();}
  assertEquals("RangeError: Negative array length -4.0", msg);

  msg = null;
  try { new Uint16Array(-268435457); } catch (e) { msg = e.toString();}
  assertEquals("RangeError: Negative array length -5.36870914E8", msg);

  one = new Uint16Array([7]);
  assertEquals("7", one.toString());

  two = new Uint16Array([7, 13]);
  assertEquals("7,13", two.toString());

  many = new Uint16Array([7,13,21,17,0,1,11]);
  assertEquals("7,13,21,17,0,1,11", many.toString());

  special = new Uint16Array([undefined, null, Number.NaN, Number.POSITIVE_INFINITY, Number.NEGATIVE_INFINITY, 4]);
  assertEquals("0,0,0,0,0,4", special.toString());

  special = new Uint16Array(6);
  special[0] = undefined;
  special[1] = null;
  special[2] = Number.NaN;
  special[3] = Number.POSITIVE_INFINITY;
  special[4] = Number.NEGATIVE_INFINITY;
  special[5] = 4;
  assertEquals("0,0,0,0,0,4", special.toString());

  udefArr = new Uint16Array(undefined);
  assertEquals(0, udefArr.length);

  nullArr = new Uint16Array(null);
  assertEquals(0, nullArr.length);


  // Int32Array
  empty = new Int32Array();
  assertEquals("", empty.toString());

  empty = new Int32Array(0);
  assertEquals("", empty.toString());

  init = new Int32Array(2);
  assertEquals("0,0", init.toString());

  msg = null;
  try { new Int32Array(-2); } catch (e) { msg = e.toString();}
  assertEquals("RangeError: Negative array length -8.0", msg);

  msg = null;
  try { new Int32Array(-268435457); } catch (e) { msg = e.toString();}
  assertEquals("RangeError: Negative array length -1.073741828E9", msg);

  one = new Int32Array([7]);
  assertEquals("7", one.toString());

  two = new Int32Array([7, 13]);
  assertEquals("7,13", two.toString());

  many = new Int32Array([7,13,21,17,0,1,11]);
  assertEquals("7,13,21,17,0,1,11", many.toString());

  special = new Int32Array([undefined, null, Number.NaN, Number.POSITIVE_INFINITY, Number.NEGATIVE_INFINITY, 4]);
  assertEquals("0,0,0,0,0,4", special.toString());

  special = new Int32Array(6);
  special[0] = undefined;
  special[1] = null;
  special[2] = Number.NaN;
  special[3] = Number.POSITIVE_INFINITY;
  special[4] = Number.NEGATIVE_INFINITY;
  special[5] = 4;
  assertEquals("0,0,0,0,0,4", special.toString());

  udefArr = new Int32Array(undefined);
  assertEquals(0, udefArr.length);

  nullArr = new Int32Array(null);
  assertEquals(0, nullArr.length);


  // Uint32Array
  empty = new Uint32Array();
  assertEquals("", empty.toString());

  empty = new Uint32Array(0);
  assertEquals("", empty.toString());

  init = new Uint32Array(2);
  assertEquals("0,0", init.toString());

  msg = null;
  try { new Uint32Array(-2); } catch (e) { msg = e.toString();}
  assertEquals("RangeError: Negative array length -8.0", msg);

  msg = null;
  try { new Uint32Array(-268435457); } catch (e) { msg = e.toString();}
  assertEquals("RangeError: Negative array length -1.073741828E9", msg);

  one = new Uint32Array([7]);
  assertEquals("7", one.toString());

  two = new Uint32Array([7, 13]);
  assertEquals("7,13", two.toString());

  many = new Uint32Array([7,13,21,17,0,1,11]);
  assertEquals("7,13,21,17,0,1,11", many.toString());

  special = new Uint32Array([undefined, null, Number.NaN, Number.POSITIVE_INFINITY, Number.NEGATIVE_INFINITY, 4]);
  assertEquals("0,0,0,0,0,4", special.toString());

  special = new Uint32Array(6);
  special[0] = undefined;
  special[1] = null;
  special[2] = Number.NaN;
  special[3] = Number.POSITIVE_INFINITY;
  special[4] = Number.NEGATIVE_INFINITY;
  special[5] = 4;
  assertEquals("0,0,0,0,0,4", special.toString());

  udefArr = new Uint32Array(undefined);
  assertEquals(0, udefArr.length);

  nullArr = new Uint32Array(null);
  assertEquals(0, nullArr.length);


  // Uint8ClampedArray
  empty = new Uint8ClampedArray();
  assertEquals("", empty.toString());

  empty = new Uint8ClampedArray(0);
  assertEquals("", empty.toString());

  init = new Uint8ClampedArray(2);
  assertEquals("0,0", init.toString());

  msg = null;
  try { new Uint8ClampedArray(-2); } catch (e) { msg = e.toString();}
  assertEquals("RangeError: Negative array length -2.0", msg);

  msg = null;
  try { new Uint8ClampedArray(-268435457); } catch (e) { msg = e.toString();}
  assertEquals("RangeError: Negative array length -2.68435457E8", msg);

  one = new Uint8ClampedArray([7]);
  assertEquals("7", one.toString());

  two = new Uint8ClampedArray([7, 13]);
  assertEquals("7,13", two.toString());

  many = new Uint8ClampedArray([7,13,21,17,0,1,11]);
  assertEquals("7,13,21,17,0,1,11", many.toString());

  special = new Uint8ClampedArray([undefined, null, Number.NaN, Number.POSITIVE_INFINITY, Number.NEGATIVE_INFINITY, 4]);
  assertEquals("0,0,0,255,0,4", special.toString());

  special = new Uint8ClampedArray(6);
  special[0] = undefined;
  special[1] = null;
  special[2] = Number.NaN;
  special[3] = Number.POSITIVE_INFINITY;
  special[4] = Number.NEGATIVE_INFINITY;
  special[5] = 4;
  assertEquals("0,0,0,255,0,4", special.toString());

  udefArr = new Uint8ClampedArray(undefined);
  assertEquals(0, udefArr.length);

  nullArr = new Uint8ClampedArray(null);
  assertEquals(0, nullArr.length);


  // Float32Array
  empty = new Float32Array();
  assertEquals("", empty.toString());

  empty = new Float32Array(0);
  assertEquals("", empty.toString());

  init = new Float32Array(2);
  assertEquals("0,0", init.toString());

  msg = null;
  try { new Float32Array(-2); } catch (e) { msg = e.toString();}
  assertEquals("RangeError: Negative array length -8.0", msg);

  msg = null;
  try { new Float32Array(-268435457); } catch (e) { msg = e.toString();}
  assertEquals("RangeError: Negative array length -1.073741828E9", msg);

  one = new Float32Array([7]);
  assertEquals("7", one.toString());

  two = new Float32Array([7, 13]);
  assertEquals("7,13", two.toString());

  many = new Float32Array([7,13,21,17,0,1,11]);
  assertEquals("7,13,21,17,0,1,11", many.toString());

  special = new Float32Array([undefined, null, Number.NaN, Number.POSITIVE_INFINITY, Number.NEGATIVE_INFINITY, 4]);
  assertEquals("NaN,0,NaN,Infinity,-Infinity,4", special.toString());

  special = new Float32Array(6);
  special[0] = undefined;
  special[1] = null;
  special[2] = Number.NaN;
  special[3] = Number.POSITIVE_INFINITY;
  special[4] = Number.NEGATIVE_INFINITY;
  special[5] = 4;
  assertEquals("NaN,0,NaN,Infinity,-Infinity,4", special.toString());

  udefArr = new Float32Array(undefined);
  assertEquals(0, udefArr.length);

  nullArr = new Float32Array(null);
  assertEquals(0, nullArr.length);


  // Float64Array
  empty = new Float64Array();
  assertEquals("", empty.toString());

  empty = new Float64Array(0);
  assertEquals("", empty.toString());

  init = new Float64Array(2);
  assertEquals("0,0", init.toString());

  msg = null;
  try { new Float64Array(-2); } catch (e) { msg = e.toString();}
  assertEquals("RangeError: Negative array length -16.0", msg);

  msg = null;
  try { new Float64Array(-268435457); } catch (e) { msg = e.toString();}
  assertEquals("RangeError: Negative array length -2.147483656E9", msg);

  one = new Float64Array([7]);
  assertEquals("7", one.toString());

  two = new Float64Array([7, 13]);
  assertEquals("7,13", two.toString());

  many = new Float64Array([7,13,21,17,0,1,11]);
  assertEquals("7,13,21,17,0,1,11", many.toString());

  special = new Float64Array([undefined, null, Number.NaN, Number.POSITIVE_INFINITY, Number.NEGATIVE_INFINITY, 4]);
  assertEquals("NaN,0,NaN,Infinity,-Infinity,4", special.toString());

  special = new Float64Array(6);
  special[0] = undefined;
  special[1] = null;
  special[2] = Number.NaN;
  special[3] = Number.POSITIVE_INFINITY;
  special[4] = Number.NEGATIVE_INFINITY;
  special[5] = 4;
  assertEquals("NaN,0,NaN,Infinity,-Infinity,4", special.toString());

  udefArr = new Float64Array(undefined);
  assertEquals(0, udefArr.length);

  nullArr = new Float64Array(null);
  assertEquals(0, nullArr.length);


// arguments
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
}

"success";