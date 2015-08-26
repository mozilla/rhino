/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'method-004-n.js';

/**
   File Name:      method-004-n.js
   Description:

   Passing arguments of the wrong type, or the wrong number of arguments,
   should cause a runtime error.

   @author     christine@netscape.com
   @version    1.00
*/
var SECTION = "LiveConnect Objects";
var VERSION = "1_3";
var TITLE   = "Passing bad arguments to a method";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

DESCRIPTION = "var string = new java.lang.String(\"\"); string.charAt(\"foo\")";
EXPECTED = "error";

var string = new java.lang.String("");

new TestCase(
  SECTION,
  "var string = new java.lang.String(\"\"); string.charAt(\"foo\")",
  "error",
  string.charAt("foo") );

test();

