/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.7.3.js';

/**
   File Name:          15.7.3.js
   15.7.3  Properties of the Number Constructor

   Description:        The value of the internal [[Prototype]] property
   of the Number constructor is the Function prototype
   object.  The Number constructor also has the internal
   [[Call]] and [[Construct]] properties, and the length
   property.

   Other properties are in subsequent tests.

   Author:             christine@netscape.com
   Date:               29 september 1997
*/

var SECTION = "15.7.3";
var VERSION = "ECMA_2";
startTest();
var TITLE   = "Properties of the Number Constructor";

writeHeaderToLog( SECTION + " "+ TITLE);

new TestCase(SECTION,
	     "Number.__proto__",  
	     Function.prototype,
	     Number.__proto__ );

test();
