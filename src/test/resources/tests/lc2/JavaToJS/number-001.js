/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'number-001.js';

/**

File Name:      number-001.js
Description:

If a Java method returns one of the primitive Java types below,
JavaScript should read the value as JavaScript number primitive.
*   byte
*   short
*   int
*   long
*   float
*   double
*   char

To test this:

1.  Call a java method that returns one of the primitive java types above.
2.  Check the value of the returned type
3.  Check the type of the returned type, which should be "number"

It is an error if the type of the JavaScript variable is "object" or if
its class is JavaObject or Number.

@author     christine@netscape.com
@version    1.00
*/
var SECTION = "LiveConnect";
var VERSION = "1_3";
var TITLE   = "Java Number Primitive to JavaScript Object";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

//  In all test cases, the expected type is "number"

var E_TYPE = "number";

//  Create arrays of actual results (java_array) and
//  expected results (test_array).

var java_array = new Array();
var test_array = new Array();

var i = 0;

//  Call a java function that returns a value whose type is int.
java_array[i] = new JavaValue( java.lang.Integer.parseInt('255') );
test_array[i] = new TestValue( "java.lang.Integer.parseInt('255')",
			       255,
			       E_TYPE );

i++;

java_array[i] = new JavaValue( (new java.lang.Double( '123456789' )).intValue() );
test_array[i] = new TestValue( "(new java.lang.Double( '123456789' )).intValue()",
			       123456789,
			       E_TYPE );

i++;

//  Call a java function that returns a value whose type is double
java_array[i] = new JavaValue( (new java.lang.Integer( '123456789' )).doubleValue() );
test_array[i] = new TestValue( "(new java.lang.Integer( '123456789' )).doubleValue()",
			       123456789,
			       E_TYPE );

// Call a java function that returns a value whose type is long
java_array[i] = new JavaValue( (new java.lang.Long('1234567891234567' )).longValue() );
test_array[i] = new TestValue( "(new java.lang.Long( '1234567891234567' )).longValue()",
			       1234567891234567,
			       E_TYPE );

// Call a java function that returns a value whose type is float

java_array[i] = new JavaValue( (new java.lang.Float( '1234.5' )).floatValue() );
test_array[i] = new TestValue( "(new java.lang.Float( '1234.5' )).floatValue()",
			       1234.5,
			       E_TYPE );

i++;

// Call a java function that returns a value whose type is char
java_array[i] = new JavaValue(  (new java.lang.String("hello")).charAt(0) );
test_array[i] = new TestValue( "(new java.lang.String('hello')).charAt(0)",
			       "h".charCodeAt(0),
			       E_TYPE );
i++;

// Call a java function that returns a value whose type is short
java_array[i] = new JavaValue(  (new java.lang.Byte(127)).shortValue() );
test_array[i] = new TestValue( "(new java.lang.Byte(127)).shortValue()",
			       127,
			       E_TYPE );
i++;

// Call a java function that returns a value whose type is byte
java_array[i] = new JavaValue( (new java.lang.Byte(127)).byteValue() );
test_array[i] = new TestValue( "(new java.lang.Byte(127)).byteValue()",
			       127,
			       E_TYPE );


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
  this.value  = value.valueOf();
  this.type   = typeof value;
  return this;
}
function TestValue( description, value, type, classname ) {
  this.description = description;
  this.value = value;
  this.type =  type;
  return this;
}
