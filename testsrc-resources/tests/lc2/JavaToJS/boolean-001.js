/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'boolean-001.js';

/**
   File Name:      boolean-001.js
   Description:

   If a Java method returns a Java boolean primitive, JavaScript should
   read the value as a JavaScript boolean primitive.

   To test this:

   1.  Call a java method that returns a Java boolean primitive.
   2.  Check the type of the returned type, which should be "boolean"
   3.  Check the value of the returned type, which should be true or false.

   It is an error if the returned value is read as a JavaScript Boolean
   object.

   @author     christine@netscape.com
   @version    1.00
*/
var SECTION = "LiveConnect";
var VERSION = "1_3";
var TITLE   = "Java Boolean Primitive to JavaScript Object";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

//  In all test cases, the expected type is "boolean"

var E_TYPE = "boolean";

//  Create arrays of actual results (java_array) and expected results
//  (test_array).

var java_array = new Array();
var test_array = new Array();

var i = 0;

// Call a java method that returns true
java_array[i] = new JavaValue(  (new java.lang.Boolean(true)).booleanValue() );
test_array[i] = new TestValue(  "(new java.lang.Boolean(true)).booleanValue()",
				true )
  i++;

// Call a java method that returns false
java_array[i] = new JavaValue(  (new java.lang.Boolean(false)).booleanValue() );
test_array[i] = new TestValue(  "(new java.lang.Boolean(false)).booleanValue()",
				false )
  i++;

for ( i = 0; i < java_array.length; i++ ) {
  CompareValues( java_array[i], test_array[i] );

}

test();

function CompareValues( javaval, testval ) {
  //  Check value
  new TestCase( SECTION,
		testval.description,
		testval.value,
		javaval.value );
  //  Check type.

  new TestCase( SECTION,
		"typeof (" + testval.description +")",
		testval.type,
		javaval.type );
}
function JavaValue( value ) {
  this.value  = value;
  this.type   = typeof value;
  return this;
}
function TestValue( description, value ) {
  this.description = description;
  this.value = value;
  this.type =  E_TYPE;
  return this;
}
