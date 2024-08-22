/*
 * This is a set of tests for array concatenation as it was implemented before
 * ES6 and the @isConcatSpreadable annotation.
 * This comes from section 15.4.4.4 of the 1999 ECMAScript spec.
 */
load("testsrc/assert.js");

function makeArrayLike(len, fromArray) {
  var al;
  if (fromArray) {
    // Create an object that has the Array constructor
    al = Object.create([]);
  } else {
    al = {};
  }
  for (var i = 0; i < len; i++) {
    al[i] = i + 1;
  }
  al.length = len;
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

// Array-like object with an array constructor, concatenated in various ways.
// Concat, which is not correct for ES6 but is for older language versions.
cr = Array.prototype.concat.call(makeArrayLike(2, true));
assertArrayEquals([1, 2], cr);
cr = makeArrayLike(2, true).concat([3, 4]);
assertArrayEquals([1, 2, 3, 4], cr);
cr = makeArrayLike(2, true).concat(makeArrayLike(2, true));
assertArrayEquals([1, 2, 1, 2], cr);
cr = makeArrayLike(2, true).concat(1, 2);
assertArrayEquals([1, 2, 1, 2], cr);
cr = [1, 2].concat(makeArrayLike(2, true));
assertArrayEquals([1, 2, 1, 2], cr);

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

'success';

