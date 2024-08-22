/*
 * This script tests some of the more complex features of arrays against both
 * array-like objects and also against native Java objects that implement the
 * contract, and finally native java arrays.
 */

load('testsrc/assert.js');

var fixedSize = false;

// Assume that there is a function in the scope called "makeTestArray()" which returns an
// array with the values ['one', 'two', 'three', 'four']. That function, in turn,
// might just call one of the functions below.

function makeNativeArray() {
  return ['one', 'two', 'three', 'four'];
}

function makeArrayLikeArray() {
  return {
    0: 'one',
    1: 'two',
    2: 'three',
    3: 'four',
    length: 4
  };
}

function makeJavaArray() {
  fixedSize = true;
  let ja = java.lang.reflect.Array.newInstance(java.lang.String, 4);
  ja[0] = 'one';
  ja[1] = 'two';
  ja[2] = 'three';
  ja[3] = 'four';
  return ja;
}

let list = makeTestArray();
assertArrayEquals(['one', 'two', 'three', 'four'], list);
assertEquals(4, list.length);

let jv = Array.prototype.join.call(list, ',');
assertEquals('one,two,three,four', jv);

if (!fixedSize) {
  // JavaArray instances can't grow and shrink like other arrays.
  list = makeTestArray();
  let pv = Array.prototype.push.call(list, 'five');
  assertArrayEquals(['one', 'two', 'three', 'four', 'five'], list);
  assertEquals(5, pv);
  pv = Array.prototype.push.call(list, '6', '7');
  assertArrayEquals(['one', 'two', 'three', 'four', 'five', '6', '7'], list);
  assertEquals(7, pv);

  list = makeTestArray();
  pv = Array.prototype.pop.call(list);
  assertArrayEquals(['one', 'two', 'three'], list);
  assertEquals('four', pv);
  pv = Array.prototype.pop.call(list);
  assertArrayEquals(['one', 'two'], list);
  assertEquals('three', pv);

  list = makeTestArray();
  let sv = Array.prototype.shift.call(list);
  assertArrayEquals(['two', 'three', 'four'], list);
  assertEquals('one', sv);
  sv = Array.prototype.shift.call(list);
  assertArrayEquals(['three', 'four'], list);
  assertEquals('two', sv);

  list = makeTestArray();
  let uv = Array.prototype.unshift.call(list, 'five');
  assertArrayEquals(['five', 'one', 'two', 'three', 'four'], list);
  assertEquals(5, uv);
  uv = Array.prototype.unshift.call(list, '6', '7');
  assertArrayEquals(['6', '7', 'five', 'one', 'two', 'three', 'four'], list);
  assertEquals(7, uv);

  list = makeTestArray();
  sv = Array.prototype.splice.call(list, 1, 2);
  assertArrayEquals(['two', 'three'], sv);
  assertEquals(2, sv.length);
  assertArrayEquals(['one', 'four'], list);
  assertEquals(2, list.length);
}

list = makeTestArray();
sv = Array.prototype.slice.call(list, 1, 3);
assertArrayEquals(['two', 'three'], sv);
assertEquals(2, sv.length);
assertArrayEquals(['one', 'two', 'three', 'four'], list);
assertEquals(4, list.length);

list = makeTestArray();
Array.prototype.sort.call(list);
assertArrayEquals(['four', 'one', 'three', 'two'], list);
assertEquals(4, list.length);

'success';
