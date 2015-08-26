/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-304828.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 304828;
var summary = 'Array Generic Methods';
var actual = '';
var expect = '';
printBugNumber(BUGNUMBER);
printStatus (summary);

var value;

// use Array methods on a String
// join
value  = '123';
expect = '1,2,3';
try
{
  actual = Array.prototype.join.call(value);
}
catch(e)
{
  actual = e + '';
}
reportCompare(expect, actual, summary + ': join');

// reverse
value  = '123';
expect = '123';
try
{
  actual = Array.prototype.reverse.call(value) + '';
}
catch(e)
{
  actual = e + '';
}
reportCompare(expect, actual, summary + ': reverse');

// sort
value  = 'cba';
expect = 'cba';
try
{
  actual = Array.prototype.sort.call(value) + '';
}
catch(e)
{
  actual = e + '';
}
reportCompare(expect, actual, summary + ': sort');

// push
value  = 'abc';
expect = 6;
try
{
  actual = Array.prototype.push.call(value, 'd', 'e', 'f');
}
catch(e)
{
  actual = e + '';
}
reportCompare(expect, actual, summary + ': push');
reportCompare('abc', value, summary + ': push');

// pop
value  = 'abc';
expect = 'c';
try
{
  actual = Array.prototype.pop.call(value);
}
catch(e)
{
  actual = e + '';
}
reportCompare(expect, actual, summary + ': pop');
reportCompare('abc', value, summary + ': pop');

// unshift
value  = 'def';
expect = 6;
try
{
  actual = Array.prototype.unshift.call(value, 'a', 'b', 'c');
}
catch(e)
{
  actual = e + '';
}
reportCompare(expect, actual, summary + ': unshift');
reportCompare('def', value, summary + ': unshift');

// shift
value  = 'abc';
expect = 'a';
try
{
  actual = Array.prototype.shift.call(value);
}
catch(e)
{
  actual = e + '';
}
reportCompare(expect, actual, summary + ': shift');
reportCompare('abc', value, summary + ': shift');

// splice
value  = 'abc';
expect = 'b';
try
{
  actual = Array.prototype.splice.call(value, 1, 1) + '';
}
catch(e)
{
  actual = e + '';
}
reportCompare(expect, actual, summary + ': splice');

// concat
value  = 'abc';
expect = 'abc,d,e,f';
try
{
  actual = Array.prototype.concat.call(value, 'd', 'e', 'f') + '';
}
catch(e)
{
  actual = e + '';
}
reportCompare(expect, actual, summary + ': concat');

// slice
value  = 'abc';
expect = 'b';
try
{
  actual = Array.prototype.slice.call(value, 1, 2) + '';
}
catch(e)
{
  actual = e + '';
}
reportCompare(expect, actual, summary + ': slice');

// indexOf
value  = 'abc';
expect = 1;
try
{
  actual = Array.prototype.indexOf.call(value, 'b');
}
catch(e)
{
  actual = e + '';
}
reportCompare(expect, actual, summary + ': indexOf');

// lastIndexOf
value  = 'abcabc';
expect = 4;
try
{
  actual = Array.prototype.lastIndexOf.call(value, 'b');
}
catch(e)
{
  actual = e + '';
}
reportCompare(expect, actual, summary + ': lastIndexOf');

// forEach
value  = 'abc';
expect = 'ABC';
actual = '';
try
{
  Array.prototype.forEach.call(value,
                               function (v, index, array)
			       {actual += array[index].toUpperCase();});
}
catch(e)
{
  actual = e + '';
}
reportCompare(expect, actual, summary + ': forEach');

// map
value  = 'abc';
expect = 'A,B,C';
try
{
  actual = Array.prototype.map.call(value,
                                    function (v, index, array)
				    {return v.toUpperCase();}) + '';
}
catch(e)
{
  actual = e + '';
}
reportCompare(expect, actual, summary + ': map');

// filter
value  = '1234567890';
expect = '2,4,6,8,0';
try
{
  actual = Array.prototype.filter.call(value,
				       function (v, index, array)
				       {return array[index] % 2 == 0;}) + '';
}
catch(e)
{
  actual = e + '';
}
reportCompare(expect, actual, summary + ': filter');

// every
value  = '1234567890';
expect = false;
try
{
  actual = Array.prototype.every.call(value,
				      function (v, index, array)
				      {return array[index] % 2 == 0;});
}
catch(e)
{
  actual = e + '';
}
reportCompare(expect, actual, summary + ': every');



