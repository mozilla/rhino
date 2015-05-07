/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'array-006-n.js';

/**
   File Name:      array-005.js
   Description:

   Put and Get JavaArray Elements

   @author     christine@netscape.com
   @version    1.00
*/
var SECTION = "LiveConnect";
var VERSION = "1_3";
var TITLE   = "Java Array to JavaScript JavaArray object";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

//  In all test cases, the expected type is "object, and the expected
//  class is "JavaArray"

var E_TYPE = "object";
var E_CLASS = "[object JavaArray]";

var byte_array = ( new java.lang.String("hi") ).getBytes();

DESCRIPTION = "byte_array[\"foo\"]";
EXPECTED = "error";

new TestCase(
  SECTION,
  "byte_array[\"foo\"]",
  void 0,
  byte_array["foo"] );

test();

