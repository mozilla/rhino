/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.8.1.8-2.js';

/**
   File Name:          15.8.1.8-2.js
   ECMA Section:       15.8.1.8.js
   Description:        All value properties of the Math object should have
   the attributes [DontEnum, DontDelete, ReadOnly]

   this test checks the DontDelete attribute of Math.SQRT2

   Author:             christine@netscape.com
   Date:               16 september 1997
*/
var SECTION = "15.8.1.8-2";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "Math.SQRT2";

writeHeaderToLog( SECTION + " "+ TITLE);

new TestCase( SECTION,
	      "delete Math.SQRT2; Math.SQRT2",
	      1.4142135623730951,    
	      eval("delete Math.SQRT2; Math.SQRT2") );

new TestCase( SECTION,
	      "delete Math.SQRT2",            
	      false,                 
	      eval("delete Math.SQRT2") );

test();
