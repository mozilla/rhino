/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'boolean-014.js';

/**
 * Preferred Argument Conversion.
 *
 * Use the syntax for explicit method invokation to override the default
 * preferred argument conversion.
 *
 */
var SECTION = "Preferred argument conversion:  boolean";
var VERSION = "1_4";
var TITLE   = "LiveConnect 3.0 JavaScript to Java Data Type Conversion " +
  SECTION;
startTest();

var TEST_CLASS = new
  Packages.com.netscape.javascript.qa.lc3.bool.Boolean_001;

// invoke method that accepts java.lang.Boolean

new TestCase(
  "TEST_CLASS[\"ambiguous(java.lang.Boolean)\"](true)",
  TEST_CLASS.BOOLEAN_OBJECT,
  TEST_CLASS["ambiguous(java.lang.Boolean)"](true) );

new TestCase(
  "TEST_CLASS[\"ambiguous(java.lang.Boolean)\"](false)",
  TEST_CLASS.BOOLEAN_OBJECT,
  TEST_CLASS["ambiguous(java.lang.Boolean)"](false) );

// invoke method that expects java.lang.Object

new TestCase(
  "TEST_CLASS[\"ambiguous(java.lang.Object)\"](true)",
  TEST_CLASS.OBJECT,
  TEST_CLASS["ambiguous(java.lang.Object)"](true) );

new TestCase(
  "TEST_CLASS[\"ambiguous(java.lang.Boolean)\"](false)",
  TEST_CLASS.OBJECT,
  TEST_CLASS["ambiguous(java.lang.Object)"](false) );

// invoke method that expects a String

new TestCase(
  "TEST_CLASS[\"ambiguous(java.lang.String)\"](true)",
  TEST_CLASS.STRING,
  TEST_CLASS["ambiguous(java.lang.String)"](true) );

new TestCase(
  "TEST_CLASS[\"ambiguous(java.lang.Boolean)\"](false)",
  TEST_CLASS.STRING,
  TEST_CLASS["ambiguous(java.lang.String)"](false) );

test();
