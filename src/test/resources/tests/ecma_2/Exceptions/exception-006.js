/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'exception-006.js';

/**
 *  File Name:          exception-006
 *  ECMA Section:
 *  Description:        Tests for JavaScript Standard Exceptions
 *
 *  ToPrimitive error.
 *
 *  Author:             christine@netscape.com
 *  Date:               31 August 1998
 */
var SECTION = "exception-006";
var VERSION = "js1_4";
var TITLE   = "Tests for JavaScript Standard Exceptions: TypeError";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

ToPrimitive_1();

test();


/**
 * Getting the [[DefaultValue]] of any instances of MyObject
 * should result in a runtime error in ToPrimitive.
 */

function MyObject() {
  this.toString = void 0;
  this.valueOf = void 0;
}

function ToPrimitive_1() {
  result = "failed: no exception thrown";
  exception = null;

  try {
    result = new MyObject() + new MyObject();
  } catch ( e ) {
    result = "passed:  threw exception",
      exception = e.toString();
  } finally {
    new TestCase(
      SECTION,
      "new MyObject() + new MyObject() [ exception is " + exception +" ]",
      "passed:  threw exception",
      result );
  }
}

