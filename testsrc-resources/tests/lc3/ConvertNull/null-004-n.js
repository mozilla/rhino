/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'null-004-n.js';

/**
 *  Preferred Argument Conversion.
 *
 *  There is no preference among Java types for converting from the jJavaScript
 *  undefined value.
 *
 */
var SECTION = "Preferred argument conversion:  null";
var VERSION = "1_4";
var TITLE   = "LiveConnect 3.0 JavaScript to Java Data Type Conversion " +
  SECTION;
startTest();

TEST_CLASS = new
  Packages.com.netscape.javascript.qa.lc3.jsnull.Null_001;

// Call an ambiguous static method without using the explicit method
// syntax should be an error.

DESCRIPTION = "Packages.com.netscape.javascript.qa.lc3.jsnull.Null_001.staticAmbiguous(null)";
EXPECTED = "error";

new TestCase(
  "Packages.com.netscape.javascript.qa.lc3.jsnull.Null_001.staticAmbiguous(null)",
  "error",
  Packages.com.netscape.javascript.qa.lc3.jsnull.Null_001.staticAmbiguous(null) );

test();
