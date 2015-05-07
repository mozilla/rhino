/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.4.3.2.js';

/**
   File Name:          15.4.3.2.js
   ECMA Section:       15.4.3.2 Array.length
   Description:        The length property is 1.

   Author:             christine@netscape.com
   Date:               7 october 1997
*/

var SECTION = "15.4.3.2";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "Array.length";

writeHeaderToLog( SECTION + " "+ TITLE);

new TestCase( SECTION,
	      "Array.length",     
	      1,       
	      Array.length );

test();
