/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'expression-008.js';

/**
   File Name:          expression-008
   Corresponds To:     11.2.2-3-n.js
   ECMA Section:       11.2.2. The new operator
   Description:
   Author:             christine@netscape.com
   Date:               12 november 1997
*/
var SECTION = "expression-008";
var VERSION = "JS1_4";
var TITLE   = "The new operator";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

var NULL = null;
var result = "Failed";
var exception = "No exception thrown";
var expect = "Passed";

try {
  result = new NULL();
} catch ( e ) {
  result = expect;
  exception = e.toString();
}

new TestCase(
  SECTION,
  "NULL = null; result = new NULL()" +
  " (threw " + exception +")",
  expect,
  result );

test();
