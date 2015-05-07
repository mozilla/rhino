/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'lexical-041.js';

/**
   File Name:          lexical-041.js
   Corresponds To:     7.5-8-n.js
   ECMA Section:       7.5 Identifiers
   Description:        Identifiers are of unlimited length
   - can contain letters, a decimal digit, _, or $
   - the first character cannot be a decimal digit
   - identifiers are case sensitive

   Author:             christine@netscape.com
   Date:               11 september 1997
*/
var SECTION = "lexical-041";
var VERSION = "ECMA_1";
var TITLE   = "Identifiers";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);
startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

var result = "Failed";
var exception = "No exception thrown";
var expect = "Passed";

try {
  eval("var @abc;");
} catch ( e ) {
  result = expect;
  exception = e.toString();
}

new TestCase(
  SECTION,
  "var @abc" +
  " (threw " + exception +")",
  expect,
  result );

test();


