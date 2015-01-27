/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'object-006.js';

/**
   File Name:      object-006.js
   Description:

   Attempt to construct a java.lang.Character.  currently this fails because of
   http://scopus/bugsplat/show_bug.cgi?id=106464

   @author     christine@netscape.com
   @version    1.00
*/

var SECTION = "LiveConnect Objects";
var VERSION = "1_3";
var TITLE   = "Construct a java.lang.Character";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

var testcase = new TestCase (
  SECTION,
  "var string = new java.lang.String(\"hi\"); "+
  "var c = new java.lang.Character(string.charAt(0)); String(c.toString())",
  "h",
  "" );

var string = new java.lang.String("hi");
var c = new java.lang.Character( string.charAt(0) );

testcase.actual = String(c.toString());

test();

