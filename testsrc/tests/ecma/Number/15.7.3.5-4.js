/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.7.3.5-4.js';

/**
   File Name:          15.7.3.5-4.js
   ECMA Section:       15.7.3.5 Number.NEGATIVE_INFINITY
   Description:        All value properties of the Number object should have
   the attributes [DontEnum, DontDelete, ReadOnly]

   this test checks the DontEnum attribute of Number.NEGATIVE_INFINITY

   Author:             christine@netscape.com
   Date:               16 september 1997
*/

var SECTION = "15.7.3.5-4";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "Number.NEGATIVE_INFINITY";

writeHeaderToLog( SECTION + " "+TITLE);

new TestCase( SECTION,
	      "var string = ''; for ( prop in Number ) { string += ( prop == 'NEGATIVE_INFINITY' ) ? prop : '' } string;",
	      "",
	      eval("var string = ''; for ( prop in Number ) { string += ( prop == 'NEGATIVE_INFINITY' ) ? prop : '' } string;")
  );

test();
