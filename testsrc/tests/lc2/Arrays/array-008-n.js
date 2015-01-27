/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'array-008-n.js';

/**
   File Name:      array-008-n.js
   Description:

   JavaArrays should have a length property that specifies the number of
   elements in the array.

   JavaArray elements can be referenced with the [] array index operator.

   This attempts to access an array index that is out of bounds.  It should
   fail with a JS runtime error.

   @author     christine@netscape.com
   @version    1.00
*/
var SECTION = "LiveConnect";
var VERSION = "1_3";
var TITLE   = "Java Array to JavaScript JavaArray object";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

dt = new Packages.com.netscape.javascript.qa.liveconnect.DataTypeClass;

var ba_length = dt.PUB_ARRAY_BYTE.length;

DESCRIPTION = "dt.PUB_ARRAY_BYTE.length = "+ ba_length;
EXPECTED = "error";

new TestCase(
  SECTION,
  "dt.PUB_ARRAY_BYTE.length = "+ ba_length,
  "error",
  dt.PUB_ARRAY_BYTE[ba_length] );

test();
