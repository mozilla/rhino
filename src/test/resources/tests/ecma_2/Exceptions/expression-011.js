/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'expression-011.js';

/**
   File Name:          expression-011.js
   Corresponds To:     ecma/Expressions/11.2.2-6-n.js
   ECMA Section:       11.2.2. The new operator
   Description:
   Author:             christine@netscape.com
   Date:               12 november 1997
*/
var SECTION = "expression-011";
var VERSION = "JS1_4";
var TITLE   = "The new operator";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

var BOOLEAN  = true;

var result = "Failed";
var exception = "No exception thrown";
var expect = "Passed";

try {
  var OBJECT = new BOOLEAN();
} catch ( e ) {
  result = expect;
  exception = e.toString();
}

new TestCase(
  SECTION,
  "BOOLEAN = true; result = new BOOLEAN()" +
  " (threw " + exception +")",
  expect,
  result );

test();

