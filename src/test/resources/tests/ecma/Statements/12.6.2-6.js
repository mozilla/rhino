/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '12.6.2-6.js';

/**
   File Name:          12.6.2-6.js
   ECMA Section:       12.6.2 The for Statement

   1. first expression is present.
   2. second expression is not present
   3. third expression is present


   Author:             christine@netscape.com
   Date:               15 september 1997
*/
var SECTION = "12.6.2-6";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "The for statement";

writeHeaderToLog( SECTION + " "+ TITLE);

new TestCase( "12.6.2-6", "for statement",  256,     testprogram() );

test();

function testprogram() {
  var myVar;

  for ( myVar=2; ; myVar *= myVar ) {

    if (myVar > 100)
      break;
    continue;
  }

  return myVar;
}
