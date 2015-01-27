/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'undefined-001-n.js';

/**
 *  Preferred Argument Conversion.
 *
 *  There is no preference among Java types for converting from the jJavaScript
 *  undefined value.
 *
 */
var SECTION = "Preferred argument conversion:  undefined";
var VERSION = "1_4";
var TITLE   = "LiveConnect 3.0 JavaScript to Java Data Type Conversion " +
  SECTION;
startTest();

var TEST_CLASS = new
  Packages.com.netscape.javascript.qa.lc3.undefined.Undefined_001;

DESCRIPTION = "TEST_CLASS.ambiguous( void 0 )";
EXPECTED = "error";

new TestCase(
  "TEST_CLASS.ambiguous( void 0 )",
  "error",
  TEST_CLASS.ambiguous(void 0) );

test();
