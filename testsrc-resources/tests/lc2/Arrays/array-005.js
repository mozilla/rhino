/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'array-005.js';

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

new TestCase(
  SECTION,
  "byte_array = new java.lang.String(\"hi\")).getBytes(); delete byte_array.length",
  false,
  delete byte_array.length );

new TestCase(
  SECTION,
  "byte_array[0]",
  ("hi").charCodeAt(0),
  byte_array[0]);

new TestCase(
  SECTION,
  "byte_array[1]",
  ("hi").charCodeAt(1),
  byte_array[1]);

byte_array.length = 0;

new TestCase(
  SECTION,
  "byte_array.length = 0; byte_array.length",
  2,
  byte_array.length );

var properties = "";
for ( var p in byte_array ) {
  properties += ( p == "length" ) ? p : "";
}

new TestCase(
  SECTION,
  "for ( var p in byte_array ) { properties += p ==\"length\" ? p : \"\" }; properties",
  "",
  properties );

new TestCase(
  SECTION,
  "byte_array[\"length\"]",
  2,
  byte_array["length"] );

byte_array["0"] = 127;

new TestCase(
  SECTION,
  "byte_array[\"0\"] = 127; byte_array[0]",
  127,
  byte_array[0] );

byte_array[1] = 99;

new TestCase(
  SECTION,
  "byte_array[1] = 99; byte_array[\"1\"]",
  99,
  byte_array["1"] );

test();

