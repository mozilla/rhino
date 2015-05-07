/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.9.5.19.js';

/**
   File Name:          15.9.5.19.js
   ECMA Section:       15.9.5.19
   Description:        Date.prototype.getUTCSeconds

   1.  Let t be this time value.
   2.  If t is NaN, return NaN.
   3.  Return SecFromTime(t).

   Author:             christine@netscape.com
   Date:               12 november 1997
*/

var SECTION = "15.9.5.19";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "Date.prototype.getUTCSeconds()";

writeHeaderToLog( SECTION + " "+ TITLE);

addTestCase( TIME_NOW );
addTestCase( TIME_0000 );
addTestCase( TIME_1970 );
addTestCase( TIME_1900 );
addTestCase( TIME_2000 );
addTestCase( UTC_FEB_29_2000 );
addTestCase( UTC_JAN_1_2005 );

new TestCase( SECTION,
	      "(new Date(NaN)).getUTCSeconds()",
	      NaN,
	      (new Date(NaN)).getUTCSeconds() );

new TestCase( SECTION,
	      "Date.prototype.getUTCSeconds.length",
	      0,
	      Date.prototype.getUTCSeconds.length );
test();

function addTestCase( t ) {
  for ( m = 0; m <= 60; m+=10 ) {
    t += 1000;
    new TestCase( SECTION,
		  "(new Date("+t+")).getUTCSeconds()",
		  SecFromTime(t),
		  (new Date(t)).getUTCSeconds() );
  }
}
