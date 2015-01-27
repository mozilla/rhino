/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'array-003.js';

/**
   File Name:      array-003.js
   Description:

   JavaArray elements should be enumerable using a for/in loop.

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

//  Create arrays of actual results (java_array) and expected results
//  (test_array).

var java_array = new Array();
var test_array = new Array();

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
				 "o".charCodeAt(0) ],
			       "[C" );
i++;

for ( i = 0; i < java_array.length; i++ ) {
  CompareValues( java_array[i], test_array[i] );
}

test();

function CompareValues( javaval, testval ) {
  //  Check value

  var p;
  var e = 0;

  for ( p in javaval.value ) {
    new TestCase( SECTION,
		  "("+ testval.description +")["+p+"]",
		  testval.value[p],
		  javaval.value[p] );
    e++;

  }

  /* Number of elements enumerated should be same as number of elements in
   * the array
   */

  new TestCase( SECTION,
		"number of elements enumerated:",
		testval.length,
		e );


  //  Check type

  new TestCase( SECTION,
		"typeof (" + testval.description +")",
		testval.type,
		javaval.type );

  //  Check class.
  new TestCase(SECTION,
               "The Java Class of ( "+ testval.description +" )",
               testval.lcclass,
               javaval.lcclass );

}
function JavaValue( value ) {
  this.value  = value;
  this.type   = typeof value;
  this.classname = this.value.toString();

  jlo_class = java.lang.Class.forName("java.lang.Object")
    jlo_getClass_method = jlo_class.getMethod("getClass", null)
    this.lcclass = jlo_getClass_method.invoke(value, null );

  return this;
}
function TestValue( description, value, lcclass ) {
  this.lcclass = java.lang.Class.forName( lcclass );
  this.description = description;
  this.length = value.length;
  this.value = value;
  this.type =  E_TYPE;
  this.classname = E_CLASS;
  return this;
}
