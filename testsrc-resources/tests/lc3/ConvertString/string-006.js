/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'string-006.js';

/**
 *  Preferred Argument Conversion.
 *
 *  Pass a JavaScript number to ambiguous method.  Use the explicit method
 *  invokation syntax to override the preferred argument conversion.
 *
 */
var SECTION = "Preferred argument conversion: string";
var VERSION = "1_4";
var TITLE   = "LiveConnect 3.0 JavaScript to Java Data Type Conversion " +
  SECTION;
startTest();

var TEST_CLASS = new
  Packages.com.netscape.javascript.qa.lc3.string.String_001;

var string = "255";

new TestCase(
  "TEST_CLASS[\"ambiguous(java.lang.String)\"](string))",
  "STRING",
  TEST_CLASS["ambiguous(java.lang.String)"](string) +'');

new TestCase(
  "TEST_CLASS[\"ambiguous(java.lang.Object)\"](string))",
  "OBJECT",
  TEST_CLASS["ambiguous(java.lang.Object)"](string) +'');

new TestCase(
  "TEST_CLASS[\"ambiguous(char)\"](string))",
  "CHAR",
  TEST_CLASS["ambiguous(char)"](string) +'');

new TestCase(
  "TEST_CLASS[\"ambiguous(double)\"](string))",
  "DOUBLE",
  TEST_CLASS["ambiguous(double)"](string) +'');

new TestCase(
  "TEST_CLASS[\"ambiguous(float)\"](string))",
  "FLOAT",
  TEST_CLASS["ambiguous(float)"](string) +'');

new TestCase(
  "TEST_CLASS[\"ambiguous(long)\"](string))",
  "LONG",
  TEST_CLASS["ambiguous(long)"](string) +'');

new TestCase(
  "TEST_CLASS[\"ambiguous(int)\"](string))",
  "INT",
  TEST_CLASS["ambiguous(int)"](string) +'');

new TestCase(
  "TEST_CLASS[\"ambiguous(short)\"](string))",
  "SHORT",
  TEST_CLASS["ambiguous(short)"](string) +'');

new TestCase(
  "TEST_CLASS[\"ambiguous(byte)\"](\"127\"))",
  "BYTE",
  TEST_CLASS["ambiguous(byte)"]("127") +'');


test();
