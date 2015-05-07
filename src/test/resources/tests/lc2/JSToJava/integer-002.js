/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'integer-002.js';

/**
   Template for LiveConnect Tests

   File Name:      number-001.js
   Description:

   When setting the value of a Java field with a JavaScript number or
   when passing a JavaScript number to a Java method as an argument,
   LiveConnect should be able to convert the number to any one of the
   following types:

   byte
   short
   int
   long
   float
   double
   char
   java.lang.Double

   Note that the value of the Java field may not be as precise as the
   JavaScript value's number, if for example, you pass a large number to
   a short or byte, or a float to integer.

   JavaScript numbers cannot be converted to instances of java.lang.Float
   or java.lang.Integer.

   This test does not cover the cases in which a Java method returns one
   of the above primitive Java types.

   Currently split up into numerous tests, since rhino live connect fails
   to translate numbers into the correct types.

   Test for passing JavasScript numbers to static java.lang.Integer methods.


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

a[i++] = new TestObject( "java.lang.Integer.toString(0)",
			 java.lang.Integer.toString(0), "0" );

//    a[i++] = new TestObject( "java.lang.Integer.toString(NaN)",
//        java.lang.Integer.toString(NaN), "NaN" );

a[i++] = new TestObject( "java.lang.Integer.toString(5)",
			 java.lang.Integer.toString(5), "5" );

a[i++] = new TestObject( "java.lang.Integer.toString(9.9)",
			 java.lang.Integer.toString(9.9), "9" );

a[i++] = new TestObject( "java.lang.Integer.toString(-9.9)",
			 java.lang.Integer.toString(-9.9), "-9" );

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
//    this.javavalue.__proto__.getJSClass = Object.prototype.toString;
//    this.jsclass = this.javavalue.getJSClass();
  return this;
}
