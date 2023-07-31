// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

load("testsrc/assert.js");

const int8 = new Int8Array([0, 10, -10, 20, -30, 40, -50]);
const int16 = new Int16Array([0x1FFFA, 0x7FFA - 0x8000,0x1FFFF]);
const u8clamped = new Uint8ClampedArray([5,10]);
const float32 = new Float32Array([1.2123410, 12.3223]);
const sparseint8 = new Int8Array([0, 1, , 3, 4, , 6]);
const sparsefloat32 = new Float32Array([0, 1, , 3, 4, , 6]); 


//basic numeric arguments 
assertEquals(0, int8.at(0))
assertEquals(-50, int8.at(-1))
assertEquals(-10, int8.at(2))

assertEquals(-6,int16.at(1));
assertEquals(-1,int16.at(-1));

assertEquals(10,u8clamped.at(-1));
assertEquals(10,u8clamped.at(1));
assertEquals(5,u8clamped.at(0));

assertEquals(float32[0],float32.at(0))
assertEquals(float32[1],float32.at(-1))

assertEquals(0,sparseint8.at(0));
assertEquals(6,sparseint8.at(-1));

assertEquals(0,sparsefloat32.at(0));
assertEquals(6,sparsefloat32.at(-1));


//it treats missing arg like 0
assertEquals(0,int8.at());

//it treats certain non numeric arguments like zero 
assertEquals(0,int8.at("a"));
assertEquals(0,int8.at({}));
assertEquals(0,int8.at([]));
assertEquals(0,int8.at(NaN));
assertEquals(0,int8.at(undefined));
assertEquals(float32[0],float32.at())

//it treats other non numerics like one
assertEquals(float32[1],float32.at("1"));
assertEquals(float32[1],float32.at(true));

//it deals with sparse arrays properly
assertEquals(0,sparseint8.at(2));
assertEquals(0,sparseint8.at(-2));

assertEquals(NaN,sparsefloat32.at(-2));
assertEquals(NaN,sparsefloat32.at(-2));


//it returns undefined for out of range arguments
assertEquals(undefined, int8.at(Infinity))
assertEquals(undefined, int8.at(11))
assertEquals(undefined, int8.at(-11))

//it acts as normal with multiple arguments
assertEquals(float32[1],float32.at(1,2,3))

//it throws when called on null or undefined
assertThrows(function() { Int16Array.prototype.at.call(null); }, TypeError);
assertThrows(function() { Int16Array.prototype.at.call(undefined); }, TypeError);

//it throws when called on other non-TypedArrays 
assertThrows(function() { Int16Array.prototype.at.call([1,2],1); }, TypeError);
assertThrows(function() { Int16Array.prototype.at.call({'foo':'bar'},1); }, TypeError);
assertThrows(function() { Int16Array.prototype.at.call(12,1)});

"success"
