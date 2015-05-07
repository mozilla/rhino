/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.8.1.5-2.js';

/**
   File Name:          15.8.1.5-2.js
   ECMA Section:       15.8.1.5.js
   Description:        All value properties of the Math object should have
   the attributes [DontEnum, DontDelete, ReadOnly]

   this test checks the DontDelete attribute of Math.LOG10E

   Author:             christine@netscape.com
   Date:               16 september 1997
*/

var SECTION = "15.8.1.5-2";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "Math.LOG10E";

writeHeaderToLog( SECTION + " "+ TITLE);

new TestCase( SECTION,
	      "delete Math.LOG10E; Math.LOG10E",  
	      0.4342944819032518,    
	      eval("delete Math.LOG10E; Math.LOG10E") );

new TestCase( SECTION,
	      "delete Math.LOG10E",               
	      false,                 
	      eval("delete Math.LOG10E") );

test();
