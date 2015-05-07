/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.7.3.6-2.js';

/**
   File Name:          15.7.3.6-2.js
   ECMA Section:       15.7.3.6 Number.POSITIVE_INFINITY
   Description:        All value properties of the Number object should have
   the attributes [DontEnum, DontDelete, ReadOnly]

   this test checks the DontDelete attribute of Number.POSITIVE_INFINITY

   Author:             christine@netscape.com
   Date:               16 september 1997
*/
var SECTION = "15.7.3.6-2";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "Number.POSITIVE_INFINITY";

writeHeaderToLog( SECTION + " "+TITLE);

new TestCase(SECTION,
	     "delete( Number.POSITIVE_INFINITY )", 
	     false,   
	     eval("delete( Number.POSITIVE_INFINITY )") );

new TestCase(SECTION,
	     "delete( Number.POSITIVE_INFINITY ); Number.POSITIVE_INFINITY",
	     Infinity, 
	     eval("delete( Number.POSITIVE_INFINITY );Number.POSITIVE_INFINITY") );

test();
