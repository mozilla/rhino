/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'object-004.js';

/**
   File Name:      object-004.js
   Description:

   Getting and Setting Java Object properties by index value.

   @author     christine@netscape.com
   @version    1.00
*/
var SECTION = "LiveConnect Objects";
var VERSION = "1_3";
var TITLE   = "Getting and setting JavaObject properties by index value";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

var vector = new java.util.Vector();

new TestCase(
  SECTION,
  "var vector = new java.util.Vector(); vector.addElement(\"hi\")",
  void 0,
  vector.addElement("hi") );

new TestCase(
  SECTION,
  "vector.elementAt(0) +''",
  "hi",
  vector.elementAt(0)+"" );

new TestCase(
  SECTION,
  "vector.setElementAt( \"hello\", 0)",
  void 0,
  vector.setElementAt( "hello", 0) );

new TestCase(
  SECTION,
  "vector.elementAt(0) +''",
  "hello",
  vector.elementAt(0)+"" );

test();

