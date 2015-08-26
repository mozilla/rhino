/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'null-001.js';

/**
   File Name:      null-001.js
   Description:

   When accessing a Java field whose value is null, JavaScript should read
   the value as the JavaScript null object.

   To test this:

   1.  Call a java method that returns the Java null value
   2.  Check the value of the returned object, which should be null
   3.  Check the type of the returned object, which should be "object"

   @author     christine@netscape.com
   @version    1.00
*/
var SECTION = "LiveConnect";
var VERSION = "1_3";
var TITLE   = "Java null to JavaScript Object";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);
//  display test information

var hashMap = new java.util.HashMap();

new TestCase(
  SECTION,
  "var hashMap = new java.util.HashMap(); hashMap.get('unknown');",
  null,
  hashMap.get("unknown") );

new TestCase(
  SECTION,
  "typeof hashMap.get('unknown')",
  "object",
  typeof hashMap.get('unknown') );



test();

function CheckType( et, at ) {
}
function CheckValue( ev, av ) {
}
