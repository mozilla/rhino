/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'boolean-002.js';

/**
 *  Preferred Argument Conversion.
 *
 *
 */
var SECTION = "Preferred argument conversion:  boolean";
var VERSION = "1_4";
var TITLE   = "LiveConnect 3.0 JavaScript to Java Data Type Conversion " +
  SECTION;
startTest();

var TEST_CLASS = new
  Packages.com.netscape.javascript.qa.lc3.bool.Boolean_002;

new TestCase(
  "TEST_CLASS.ambiguous( true )",
  TEST_CLASS.expect(),
  TEST_CLASS.ambiguous(true) );

new TestCase(
  "TEST_CLASS.ambiguous( false )",
  TEST_CLASS.expect(),
  TEST_CLASS.ambiguous( false ) );

test();
