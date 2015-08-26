/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'expression-013.js';

/**
   File Name:          expression-013.js
   Corresponds To:     ecma/Expressions/11.2.2-8-n.js
   ECMA Section:       11.2.2. The new operator
   Description:
   Author:             christine@netscape.com
   Date:               12 november 1997
*/
var SECTION = "expression-013";
var VERSION = "JS1_4";
var TITLE   = "The new operator";
var BUGNUMBER= "327765";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

var NUMBER = new Number(1);

var result = "Failed";
var exception = "No exception thrown";
var expect = "Passed";

try {
  result = new NUMBER();
} catch ( e ) {
  result = expect;
  exception = e.toString();
}

new TestCase(
  SECTION,
  "NUMBER = new Number(1); result = new NUMBER()" +
  " (threw " + exception +")",
  expect,
  result );

test();

