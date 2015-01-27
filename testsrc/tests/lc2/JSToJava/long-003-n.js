/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'long-003-n.js';

/**
   Template for LiveConnect Tests

   File Name:      number-001.js
   Description:

   This test fails in lc3, but will succeed if the underlying version
   of liveconnect only supports LC2.

   @author     christine@netscape.com
   @version    1.00
*/
var SECTION = "LiveConnect";
var VERSION = "1_3";
var TITLE   = "LiveConnect JavaScript to Java Data Type Conversion";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

// typeof all resulting objects is "object";
var E_TYPE = "object";

// JS class of all resulting objects is "JavaObject";
var E_JSCLASS = "[object JavaObject]";

var a = new Array();
var i = 0;

a[i++] = new TestObject( "java.lang.Long.toString(NaN)",
			 java.lang.Long.toString(NaN), "0" );

for ( var i = 0; i < a.length; i++ ) {

  // check typeof
  new TestCase(
    SECTION,
    "typeof (" + a[i].description +")",
    a[i].type,
    typeof a[i].javavalue );
/*
// check the js class
new TestCase(
SECTION,
"("+ a[i].description +").getJSClass()",
E_JSCLASS,
a[i].jsclass );
*/
  // check the number value of the object
  new TestCase(
    SECTION,
    "String(" + a[i].description +")",
    a[i].jsvalue,
    String( a[i].javavalue ) );
}

test();

function TestObject( description, javavalue, jsvalue ) {
  this.description = description;
  this.javavalue = javavalue;
  this.jsvalue = jsvalue;
  this.type = E_TYPE;
//  LC2 does not support the __proto__ property in Java objects.
//    this.javavalue.__proto__.getJSClass = Object.prototype.toString;
//    this.jsclass = this.javavalue.getJSClass();
  return this;
}
