/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'boolean-012-n.js';

/**
 *  Preferred Argument Conversion.
 *
 * It is an error to pass a JavaScript boolean to a method that expects
 * a primitive Java numeric type.
 *
 */
var SECTION = "Preferred argument conversion:  boolean";
var VERSION = "1_4";
var TITLE   = "LiveConnect 3.0 JavaScript to Java Data Type Conversion " +
  SECTION;
startTest();

var TEST_CLASS = new
  Packages.com.netscape.javascript.qa.lc3.bool.Boolean_004;

DESCRIPTION = "TEST_CLASS[\"ambiguous(double)\"](true)";
EXPECTED = "error";

new TestCase(
  "TEST_CLASS[\"ambiguous(double)\"](true)",
  "error",
  TEST_CLASS["ambiguous(double)"](true) );

test();
