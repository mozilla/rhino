/*
 * This is a set of tests for array concatenation in ES6.
 */
'use strict'; 

load("testsrc/assert.js");

function makeArrayLike(len, concatSpreadable) {
  var al = {};
  for (var i = 0; i < len; i++) {
    al[i] = i + 1;
  }
  al.length = len;
  if (concatSpreadable) {
    al[Symbol.isConcatSpreadable] = true;
  }
  return al;
}

function makeArrayObject(len) {
  var al = new Array(len);
  for (var i = 0; i < len; i++) {
    al[i] = i + 1;
  }
  return al;
}

function makeJavaArray(len) {
  var ja = java.lang.reflect.Array.newInstance(java.lang.Integer, len);
  for (var i = 0; i < len; i++) {
    ja[i] = i + 1;
  }
  return ja;
}

// Existing test case, now being run in a different way
var a = ['a0', 'a1'];
a[3] = 'a3';
var b = ['b1', 'b2'];
var cr = b.concat(a);
assertArrayEquals(['b1', 'b2', 'a0', 'a1', undefined, 'a3'], cr);

// Two native arrays
cr = [1, 2, 3].concat([4, 5, 6]);
assertArrayEquals([1, 2, 3, 4, 5, 6], cr);

// Native array plus individual elements
cr = [1, 2, 3].concat('four', 'five');
assertArrayEquals([1, 2, 3, 'four', 'five'], cr);

// Native array plus object
var no = {
  foo: 'bar'
};
cr = ['foo'].concat(no);
assertArrayEquals(['foo', no], cr);

// Native array plus array-like object -- do not concat
var al = makeArrayLike(4);
cr = [1, 2, 3].concat(al);
assertArrayEquals([1, 2, 3, al], cr);

// Array-like object plus native array -- do not concat
al = makeArrayLike(2);
cr = Array.prototype.concat.call(al, [3, 4]);
assertArrayEquals([al, 3, 4], cr);

// Non-array plus native array
cr = Array.prototype.concat.call('one', [2, 3]);
// ToObject conversion makes this comparision different
assertEquals(3, cr.length);
assertEquals('object', typeof cr[0]);
assertTrue('one' == cr[0]);
assertEquals(2, cr[1]);
assertEquals(3, cr[2]);

// Three non-arrays
cr = Array.prototype.concat.call('one', 'two', 'three');
assertEquals(3, cr.length);
assertEquals('object', typeof cr[0]);
assertTrue('one' == cr[0]);
assertEquals('two', cr[1]);
assertEquals('three', cr[2]);

// Array-liks objects with a @isConcatSpreadable symbol
cr = Array.prototype.concat.call(makeArrayLike(2, true));
assertArrayEquals([1, 2], cr);
cr = Array.prototype.concat.call(makeArrayLike(2, true), [3, 4]);
assertArrayEquals([1, 2, 3, 4], cr);
cr = Array.prototype.concat.call(makeArrayLike(2, true), makeArrayLike(2, true));
assertArrayEquals([1, 2, 1, 2], cr);
cr = Array.prototype.concat.call(makeArrayLike(2, true), 1, 2);
assertArrayEquals([1, 2, 1, 2], cr);
cr = [1, 2].concat(makeArrayLike(2, true));
assertArrayEquals([1, 2, 1, 2], cr);

// Native array plus Java array. Java arrays are "arrays" for the purposes of this
// function, and this is how things functioned in previous Rhino releases.
cr = [1, 2].concat(makeJavaArray(2));
// Java object conversion makes array comparision harder
assertEquals(4, cr.length);
assertTrue(cr[0] == 1);
assertTrue(cr[1] == 2);
assertTrue(cr[2] == 1);
assertTrue(cr[3] == 2);

// Java array plus native array
cr = Array.prototype.concat.call(makeJavaArray(2), [3, 4]);
assertEquals(4, cr.length);
assertTrue(cr[0] == 1);
assertTrue(cr[1] == 2);
assertTrue(cr[2] == 3);
assertTrue(cr[3] == 4);

// Two java arrays
cr = Array.prototype.concat.call(makeJavaArray(2), makeJavaArray(3));
assertEquals(5, cr.length);
assertTrue(cr[0] == 1);
assertTrue(cr[1] == 2);
assertTrue(cr[2] == 1);
assertTrue(cr[3] == 2);
assertTrue(cr[4] == 3);

// A few operations on the Array object
cr = new Array(3).concat();
assertArrayEquals([undefined, undefined, undefined], cr);
cr = makeArrayObject(3).concat();
assertArrayEquals([1, 2, 3], cr);
cr = makeArrayObject(3).concat([4, 5, 6]);
assertArrayEquals([1, 2, 3, 4, 5, 6], cr);
cr = makeArrayObject(1).concat(makeArrayLike(2, false));
assertEquals(2, cr.length);
assertEquals(1, cr[0]);
assertEquals('object', typeof cr[1]);
cr = makeArrayObject(3).concat(makeArrayLike(2, true));
assertArrayEquals([1, 2, 3, 1, 2], cr);

// Ensure that the Array object lets us change the prototype
let getterCalled = false;
let proto = {};
Object.defineProperty(proto, 0, {
  get: () => {
    getterCalled = true;
    return 'z';
  }
});

a = new Array(3);
assertEquals(undefined, a[0]);
Object.setPrototypeOf(a, proto);
assertEquals('z', a[0]);
assertTrue(getterCalled);
getterCalled = false;
cr = Array.prototype.concat.call(a);
assertArrayEquals(['z', undefined, undefined], cr);
assertTrue(getterCalled);

'success';

