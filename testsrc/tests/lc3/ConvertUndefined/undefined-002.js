/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'undefined-002.js';

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

new TestCase(
  "TEST_CLASS[\"ambiguous(java.lang.Object)\"](void 0) +''",
  "OBJECT",
  TEST_CLASS["ambiguous(java.lang.Object)"](void 0) +'' );


new TestCase(
  "TEST_CLASS[\"ambiguous(java.lang.String)\"](void 0) +''",
  "STRING",
  TEST_CLASS["ambiguous(java.lang.String)"](void 0) +'' );

test();
