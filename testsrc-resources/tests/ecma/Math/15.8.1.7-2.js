/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.8.1.7-2.js';

/**
   File Name:          15.8.1.7-2.js
   ECMA Section:       15.8.1.7.js
   Description:        All value properties of the Math object should have
   the attributes [DontEnum, DontDelete, ReadOnly]

   this test checks the DontDelete attribute of Math.SQRT1_2

   Author:             christine@netscape.com
   Date:               16 september 1997
*/

var SECTION = "15.8.1.7-2";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "Math.SQRT1_2";

writeHeaderToLog( SECTION + " "+ TITLE);

new TestCase( SECTION,
	      "delete Math.SQRT1_2; Math.SQRT1_2",
	      0.7071067811865476,
	      eval("delete Math.SQRT1_2; Math.SQRT1_2") );

new TestCase( SECTION,
	      "delete Math.SQRT1_2",               
	      false,             
	      eval("delete Math.SQRT1_2") );

test();
