// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

load("testsrc/assert.js");

assertEquals(1,Array.prototype.at.length)

const alphaArray = ["b","c","d"];
const intArray = new Array(0, 10, -10, 20, -30, 40, -50);
const a = [0, 1, , 3, 4, , 6];

//basic numeric arguments
assertEquals("b", alphaArray.at(0))
assertEquals("d", alphaArray.at(-1))
assertEquals({a:"b"}, [{a:"b"}, {c: "d"}].at(0))
assertEquals(0, intArray.at(0))
assertEquals(-50, intArray.at(-1))
assertEquals(-10, intArray.at(2))

//it treats missing arg like 0
assertEquals(0,intArray.at());

//it treats certain non numerics like 0
assertEquals(0,intArray.at("a"));
assertEquals(0,intArray.at({}));
assertEquals(0,intArray.at(null));
assertEquals(0,intArray.at(function(){}));
assertEquals(0,intArray.at(undefined));

//it treats other non numerics like 1
assertEquals(10,intArray.at(true));
assertEquals(10,intArray.at("1"));

//it returns undefined for non-populated elems in sparse arrays
assertEquals(undefined,a.at(2));
assertEquals(undefined,a.at(5));

//it returns undefined for out-of-range arguments
assertEquals(undefined, intArray.at(Infinity))
assertEquals(undefined, intArray.at(11))

//it deals properly with array-likes
var arrayLike = {
  0x80000000: 'Integer.MAX_VALUE + 1',
  length: 0x80000001  // Integer.MAX_VALUE + 2
};
assertEquals('Integer.MAX_VALUE + 1',Array.prototype.at.call(arrayLike, 0x80000000)); 

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
var al = makeArrayLike(2,[1,2]);
assertEquals(1,al.at(0));

//it performs as expected with integers and strings
assertEquals("9",Array.prototype.at.call("123456789",-1));
assertEquals(undefined,Array.prototype.at.call(123456789,-1));
assertEquals(undefined,Array.prototype.at.call({'foo':'bar'},0));

//it throws when called on null or undefined
assertThrows(function() { Array.prototype.at.call(null); }, TypeError);
assertThrows(function() { Array.prototype.at.call(undefined); }, TypeError);

"success"