/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'array-001.js';

/**
   File Name:      array-001.js
   Description:

   When accessing a Java field whose value is a java.lang.Array,
   or if a java method returns a java.lang.Array, JavaScript should
   read the value as the JavaScript JavaArray, an object whose class
   is JavaArray.

   To test this:

   1.  Call a java method that returns a java.lang.Array
   2.  Check the value of the returned object, which should be the value
   of the array.  Iterate through the array indices.
   3.  Check the type of the returned object, which should be "object"
   4.  Check the class of the object, using Object.prototype.toString,
   which should be "[object JavaArray]"
   5.  Check the class of the JavaArray, using getClass, and compare
   it to java.lang.Class.forName("java.lang.Array");

   @author     christine@netscape.com
   @version    1.00
*/
var SECTION = "LiveConnect";
var VERSION = "1_3";
var TITLE   = "Java Array to JavaScript JavaArray object";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

//  In all test cases, the expected type is "object, and the expected
//  class is "Number"

var E_TYPE = "object";
var E_CLASS = "[object JavaArray]";
var test_array = new Array();

//  Create arrays of actual results (java_array) and expected results
//  (test_array).

var java_array = new Array();

var i = 0;

// byte[]

var byte_array = ( new java.lang.String("hello") ).getBytes();

java_array[i] = new JavaValue( byte_array );
test_array[i] = new TestValue( "( new java.lang.String('hello') ).getBytes()",
			       ["h".charCodeAt(0),
				"e".charCodeAt(0),
				"l".charCodeAt(0),
				"l".charCodeAt(0),
				"o".charCodeAt(0) ],
			       "[B"
  );
i++;

// char[]
var char_array = ( new java.lang.String("rhino") ).toCharArray();

java_array[i] = new JavaValue( char_array );
test_array[i] = new TestValue( "( new java.lang.String('rhino') ).toCharArray()",
			       [ "r".charCodeAt(0),
				 "h".charCodeAt(0),
				 "i".charCodeAt(0),
				 "n".charCodeAt(0),
				 "o".charCodeAt(0)],
			       "[C");
i++;


for ( i = 0; i < java_array.length; i++ ) {
  CompareValues( java_array[i], test_array[i] );
}

test();

function CompareValues( javaval, testval ) {
  //  Check value

  for ( var i = 0; i < testval.value.length; i++ ) {
    new TestCase( SECTION,
		  "("+ testval.description +")["+i+"]",
		  testval.value[i],
		  javaval.value[i] );
  }

  //  Check type

  new TestCase( SECTION,
		"typeof (" + testval.description +")",
		testval.type,
		javaval.type );


  //  Check class
  new TestCase(SECTION,
               "The Java Class of ( "+ testval.description +" )",
               testval.lcclass +"",
               javaval.lcclass +"");

}
function JavaValue( value ) {
  this.value  = value;
  this.type   = typeof value;
  this.classname = this.value.getClass();

  jlo_class = java.lang.Class.forName("java.lang.Object")
    jlo_getClass_method = jlo_class.getMethod("getClass", null)
    this.lcclass = jlo_getClass_method.invoke(value, null );

  return this;
}
function TestValue( description, value, lcclass ) {
  this.lcclass = java.lang.Class.forName( lcclass );
  this.description = description;
  this.value = value;
  this.type =  E_TYPE;
  this.classname = E_CLASS;
  return this;
}
