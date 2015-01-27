/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'char-001.js';

/**
   File Name:      char-001.js
   Description:

   Java methods that return a char value should be read as a
   JavaScript number.

   To test this:

   1.  Call a method that returns a char.
   2.  Set the value of a JavaScript variable to the char value.
   3.  Check the value of the returned type, which should be "number"
   4.  Check the type of the returned type, which should be the Unicode
   encoding of that character.

   It is an error if the JavaScript variable is an object, or JavaObject
   whose class is java.lang.Character.

   @author     christine@netscape.com
   @version    1.00
*/
var SECTION = "LiveConnect";
var VERSION = "1_3";
var TITLE   = "Java char return value to JavaScript Object";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

//  In all cases, the expected type is "number"
var E_TYPE = "number";

//  Create arrays of actual results (java_array) and expected results
//  (test_array).

var java_array = new Array();
var test_array = new Array();

var i = 0;

// Get a char using String.charAt()

java_array[i] = new JavaValue(  (new java.lang.String( "JavaScript" )).charAt(0)   );
test_array[i] = new TestValue(  "(new java.lang.String( 'JavaScript' )).charAt(0)",
				74 );

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
  //  Check type, which should be E_TYPE
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
