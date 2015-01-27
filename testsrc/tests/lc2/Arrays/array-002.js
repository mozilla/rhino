/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'array-002.js';

/**
   File Name:      array-002.js
   Description:

   JavaArrays should have a length property that specifies the number of
   elements in the array.

   JavaArray elements can be referenced with the [] array index operator.

   @author     christine@netscape.com
   @version    1.00
*/
var SECTION = "LiveConnect";
var VERSION = "1_3";
var TITLE   = "Java Array to JavaScript JavaArray object";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

//  In all test cases, the expected type is "object, and the expected
//  class is "JavaArray"

var E_TYPE = "object";
var E_CLASS = "[object JavaArray]";

//  Create arrays of actual results (java_array) and expected results
//  (test_array).

var java_array = new Array();
var test_array = new Array();

var i = 0;

// byte[]

var byte_array = ( new java.lang.String("ABCDEFGHIJKLMNOPQRSTUVWXYZ") ).getBytes();

java_array[i] = new JavaValue( byte_array );
test_array[i] = new TestValue( "( new java.lang.String('ABCDEFGHIJKLMNOPQRSTUVWXYZ') ).getBytes()",
			       "ABCDEFGHIJKLMNOPQRSTUVWXYZ".length
  );
i++;


// char[]
var char_array = ( new java.lang.String("rhino") ).toCharArray();

java_array[i] = new JavaValue( char_array );
test_array[i] = new TestValue( "( new java.lang.String('rhino') ).toCharArray()",
			       "rhino".length );
i++;


for ( i = 0; i < java_array.length; i++ ) {
  CompareValues( java_array[i], test_array[i] );
}

test();

function CompareValues( javaval, testval ) {
  //  Check length
  new TestCase( SECTION,
		"("+ testval.description +").length",
		testval.value,
		javaval.length );
}
function JavaValue( value ) {
  this.value  = value;
  this.length = value.length;
  this.type   = typeof value;
  this.classname = this.value.toString();

  return this;
}
function TestValue( description, value ) {
  this.description = description;
  this.length = value
    this.value = value;
  this.type =  E_TYPE;
  this.classname = E_CLASS;
  return this;
}
