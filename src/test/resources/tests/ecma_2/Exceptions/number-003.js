/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'number-003.js';

/**
   File Name:          number-003.js
   Corresponds To:     15.7.4.3-3.js
   ECMA Section:       15.7.4.3.1 Number.prototype.valueOf()
   Description:
   Returns this number value.

   The valueOf function is not generic; it generates a runtime error if its
   this value is not a Number object. Therefore it cannot be transferred to
   other kinds of objects for use as a method.

   Author:             christine@netscape.com
   Date:               16 september 1997
*/
var SECTION = "number-003";
var VERSION = "JS1_4";
var TITLE   = "Exceptions for Number.valueOf()";

startTest();
writeHeaderToLog( SECTION + " Number.prototype.valueOf()");

var result = "Failed";
var exception = "No exception thrown";
var expect = "Passed";

try {
  VALUE_OF = Number.prototype.valueOf;
  OBJECT = new String("Infinity");
  OBJECT.valueOf = VALUE_OF;
  result = OBJECT.valueOf();
} catch ( e ) {
  result = expect;
  exception = e.toString();
}

new TestCase(
  SECTION,
  "Assigning Number.prototype.valueOf as the valueOf of a String object " +
  " (threw " + exception +")",
  expect,
  result );

test();

