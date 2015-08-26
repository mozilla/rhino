/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'null-002.js';

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

// pass null to a static method that expects an Object with explicit
// method syntax.

new TestCase(
  "java.lang.String[\"valueOf(java.lang.Object)\"](null) +''",
  "null",
  java.lang.String["valueOf(java.lang.Object)"](null) + "" );

// Pass null to a static method that expects a string without explicit
// method syntax.  In this case, there is only one matching method.

new TestCase(
  "java.lang.Boolean.valueOf(null) +''",
  "false",
  java.lang.Boolean.valueOf(null) +"" );

// Pass null to a static method that expects a string  using explicit
// method syntax.

new TestCase(
  "java.lang.Boolean[\"valueOf(java.lang.String)\"](null)",
  "false",
  java.lang.Boolean["valueOf(java.lang.String)"](null) +"" );

test();
