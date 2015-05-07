/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.4.4.js';

/**
   File Name:          15.4.4.js
   ECMA Section:       15.4.4 Properties of the Array Prototype Object
   Description:        The value of the internal [[Prototype]] property of
   the Array prototype object is the Object prototype
   object.

   Note that the Array prototype object is itself an
   array; it has a length property (whose initial value
   is (0) and the special [[Put]] method.

   Author:             christine@netscape.com
   Date:               7 october 1997
*/

var SECTION = "15.4.4";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "Properties of the Array Prototype Object";

writeHeaderToLog( SECTION + " "+ TITLE);


new TestCase( SECTION,	"Array.prototype.length",   0,          Array.prototype.length );

//  verify that prototype object is an Array object.
new TestCase( SECTION,	"typeof Array.prototype",    "object",   typeof Array.prototype );

new TestCase( SECTION,
	      "Array.prototype.toString = Object.prototype.toString; Array.prototype.toString()",
	      "[object Array]",
	      eval("Array.prototype.toString = Object.prototype.toString; Array.prototype.toString()") );

test();
