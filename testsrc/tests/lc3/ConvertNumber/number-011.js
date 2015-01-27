/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'number-011.js';

/**
 *  Preferred Argument Conversion.
 *
 *  Use the explicit method invocation syntax to override the preferred
 *  argument conversion.
 *
 */
var SECTION = "Preferred argument conversion:  undefined";
var VERSION = "1_4";
var TITLE   = "LiveConnect 3.0 JavaScript to Java Data Type Conversion " +
  SECTION;
startTest();

var TEST_CLASS = new
  Packages.com.netscape.javascript.qa.lc3.number.Number_001;

new TestCase(
  "TEST_CLASS[\"ambiguous(java.lang.Object)\"](1)",
  "OBJECT",
  TEST_CLASS["ambiguous(java.lang.Object)"](1) +'');


new TestCase(
  "TEST_CLASS[\"ambiguous(java.lang.String)\"](1)",
  "STRING",
  TEST_CLASS["ambiguous(java.lang.String)"](1) +'');

new TestCase(
  "TEST_CLASS[\"ambiguous(byte)\"](1)",
  "BYTE",
  TEST_CLASS["ambiguous(byte)"](1) +'');

new TestCase(
  "TEST_CLASS[\"ambiguous(char)\"](1)",
  "CHAR",
  TEST_CLASS["ambiguous(char)"](1) +'');

new TestCase(
  "TEST_CLASS[\"ambiguous(short)\"](1)",
  "SHORT",
  TEST_CLASS["ambiguous(short)"](1) +'');

new TestCase(
  "TEST_CLASS[\"ambiguous(int)\"](1)",
  "INT",
  TEST_CLASS["ambiguous(int)"](1) +'');

new TestCase(
  "TEST_CLASS[\"ambiguous(long)\"](1)",
  "LONG",
  TEST_CLASS["ambiguous(long)"](1) +'');

new TestCase(
  "TEST_CLASS[\"ambiguous(float)\"](1)",
  "FLOAT",
  TEST_CLASS["ambiguous(float)"](1) +'');

new TestCase(
  "TEST_CLASS[\"ambiguous(java.lang.Double)\"](1)",
  "DOUBLE_OBJECT",
  TEST_CLASS["ambiguous(java.lang.Double)"](1) +'');

new TestCase(
  "TEST_CLASS[\"ambiguous(double)\"](1)",
  "DOUBLE",
  TEST_CLASS["ambiguous(double)"](1) +'');

test();
