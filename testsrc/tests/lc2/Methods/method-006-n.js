/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'method-006-n.js';

/**
   File Name:      method-006-n.js
   Description:

   Assigning a Java method to a JavaScript object should not change the
   context associated with the Java method -- its this object should
   be the Java object, not the JavaScript object.

   That is from Flanagan.  In practice, this fails all implementations.

   @author     christine@netscape.com
   @version    1.00
*/
var SECTION = "LiveConnect Objects";
var VERSION = "1_3";
var TITLE   = "Assigning a Non-Static Java Method to a JavaScript Object";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

var java_string = new java.lang.String("LiveConnect");
var js_string   = "JavaScript";

DESCRIPTION = "var java_string = new java.lang.String(\"LiveConnect\");" +
  "var js_string = \"JavaScript\"" +
  "js_string.startsWith = java_string.startsWith"+
  "js_string.startsWith(\"J\")";

EXPECTED = "error";

js_string.startsWith = java_string.startsWith;

new TestCase(
  SECTION,
  "var java_string = new java.lang.String(\"LiveConnect\");" +
  "var js_string = \"JavaScript\"" +
  "js_string.startsWith = java_string.startsWith"+
  "js_string.startsWith(\"J\")",
  false,
  js_string.startsWith("J") );

test();

function MyObject() {
  this.println = java.lang.System.out.println;
  this.classForName = java.lang.Class.forName;
  return this;
}

