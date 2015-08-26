/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'exception-009.js';

/**
 *  File Name:          exception-009
 *  ECMA Section:
 *  Description:        Tests for JavaScript Standard Exceptions
 *
 *  Regression test for nested try blocks.
 *
 *  http://scopus.mcom.com/bugsplat/show_bug.cgi?id=312964
 *
 *  Author:             christine@netscape.com
 *  Date:               31 August 1998
 */
var SECTION = "exception-009";
var VERSION = "JS1_4";
var TITLE   = "Tests for JavaScript Standard Exceptions: SyntaxError";
var BUGNUMBER= "312964";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

try {
  expect = "passed:  no exception thrown";
  result = expect;
  Nested_1();
} catch ( e ) {
  result = "failed: threw " + e;
} finally {
  new TestCase(
    SECTION,
    "nested try",
    expect,
    result );
}


test();

function Nested_1() {
  try {
    try {
    } catch (a) {
    } finally {
    }
  } catch (b) {
  } finally {
  }
}
