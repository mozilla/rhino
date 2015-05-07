/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'string-003.js';

/**
 *  Preferred Argument Conversion.
 *
 *  Pass a JavaScript number to ambiguous method.
 *
 */
var SECTION = "Preferred argument conversion: string";
var VERSION = "1_4";
var TITLE   = "LiveConnect 3.0 JavaScript to Java Data Type Conversion " +
  SECTION;
startTest();

var TEST_CLASS = new
  Packages.com.netscape.javascript.qa.lc3.string.String_003;

var string = "J";

new TestCase(
  "TEST_CLASS.ambiguous(string)",
  TEST_CLASS.expect(),
  TEST_CLASS.ambiguous(string) );

test();
