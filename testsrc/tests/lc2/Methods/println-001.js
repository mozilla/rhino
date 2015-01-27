/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'println-001.js';

/**
   File Name:          println-001.js
   Section:       LiveConnect
   Description:

   Regression test for
   http://scopus.mcom.com/bugsplat/show_bug.cgi?id=114820

   Verify that java.lang.System.out.println does not cause an error.
   Not sure how to get to the error any other way.

   Author:             christine@netscape.com
   Date:               12 november 1997
*/
var SECTION = "println-001.js";
var VERSION = "JS1_3";
var TITLE   = "java.lang.System.out.println";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

new TestCase(
  SECTION,
  "java.lang.System.out.println( \"output from test live/Methods/println-001.js\")",
  void 0,
  java.lang.System.out.println( "output from test live/Methods/println-001.js" ) );

test();

