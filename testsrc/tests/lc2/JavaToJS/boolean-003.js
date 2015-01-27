/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'boolean-003.js';

/**
   File Name:      boolean-005.js
   Description:

   A java.lang.Boolean object field should be read as a JavaScript JavaObject.

   @author     christine@netscape.com
   @version    1.00
*/
var SECTION = "LiveConnect";
var VERSION = "1_3";
var TITLE   = "Java Boolean Object to JavaScript Object";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

//  In all test cases, the expected type is "object"

var E_TYPE = "object";

//  The JavaScrpt [[Class]] of a JavaObject should be JavaObject"

var E_JSCLASS = "[object JavaObject]";

//  The Java class of this object is java.lang.Boolean.

var E_JAVACLASS = java.lang.Class.forName( "java.lang.Boolean" );

//  Create arrays of actual results (java_array) and expected results
//  (test_array).

var java_array = new Array();
var test_array = new Array();

var i = 0;

// Test for java.lang.Boolean.FALSE, which is a Boolean object.
java_array[i] = new JavaValue(  java.lang.Boolean.FALSE  );
test_array[i] = new TestValue(  "java.lang.Boolean.FALSE",
				false );

i++;

// Test for java.lang.Boolean.TRUE, which is a Boolean object.
java_array[i] = new JavaValue(  java.lang.Boolean.TRUE  );
test_array[i] = new TestValue(  "java.lang.Boolean.TRUE",
				true );

i++;

for ( i = 0; i < java_array.length; i++ ) {
  CompareValues( java_array[i], test_array[i] );

}

test();

function CompareValues( javaval, testval ) {
  //  Check booleanValue()
  new TestCase( SECTION,
		"("+testval.description+").booleanValue()",
		testval.value,
		javaval.value );
  //  Check typeof, which should be E_TYPE
  new TestCase( SECTION,
		"typeof (" + testval.description +")",
		testval.type,
		javaval.type );
/*
//  Check JavaScript class, which should be E_JSCLASS
new TestCase( SECTION,
"(" + testval.description +").getJSClass()",
testval.jsclass,
javaval.jsclass );
*/
  //  Check Java class, which should equal() E_JAVACLASS
  new TestCase( SECTION,
		"(" + testval.description +").getClass().equals( " + E_JAVACLASS +" )",
		true,
		javaval.javaclass.equals( testval.javaclass ) );
}
function JavaValue( value ) {
  //  java.lang.Object.getClass() returns the Java Object's class.
  this.javaclass = value.getClass();

  // Object.prototype.toString will show its JavaScript wrapper object.
//    value.__proto__.getJSClass = Object.prototype.toString;
//    this.jsclass = value.getJSClass();

  this.value  = value.booleanValue();
  this.type   = typeof value;
  return this;
}
function TestValue( description, value ) {
  this.description = description;
  this.value = value;
  this.type =  E_TYPE;
  this.javaclass = E_JAVACLASS;
  this.jsclass = E_JSCLASS;
  return this;
}
