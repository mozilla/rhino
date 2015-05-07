/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'method-001.js';

/**
   File Name:      method-001.js
   Description:

   Call a static method of an object and verify return value.
   This is covered more thoroughly in the type conversion test cases.
   This only covers cases in which JavaObjects are returned.

   @author     christine@netscape.com
   @version    1.00
*/
var SECTION = "LiveConnect Objects";
var VERSION = "1_3";
var TITLE   = "Calling Static Methods";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

//  All JavaObjects are of the type "object"

var E_TYPE = "object";

//  All JavaObjects [[Class]] property is JavaObject
var E_JSCLASS = "[object JavaObject]";

//  Create arrays of actual results (java_array) and
//  expected results (test_array).

var java_array = new Array();
var test_array = new Array();

var i = 0;

java_array[i] = new JavaValue(  java.lang.String.valueOf(true)  );
test_array[i] = new TestValue(  "java.lang.String.valueOf(true)",
				"object", "java.lang.String", "true" );

i++;

java_array[i] = new JavaValue( java.awt.Color.getHSBColor(0.0, 0.0, 0.0) );
test_array[i] = new TestValue( "java.awt.Color.getHSBColor(0.0, 0.0, 0.0)",
			       "object", "java.awt.Color", "java.awt.Color[r=0,g=0,b=0]" );

i++;


for ( i = 0; i < java_array.length; i++ ) {
  CompareValues( java_array[i], test_array[i] );
}

test();

function CompareValues( javaval, testval ) {
  //  Check type, which should be E_TYPE
  new TestCase( SECTION,
		"typeof (" + testval.description +" )",
		testval.type,
		javaval.type );
/*
//  Check JavaScript class, which should be E_JSCLASS
new TestCase( SECTION,
"(" + testval.description +" ).getJSClass()",
E_JSCLASS,
javaval.jsclass );
*/
  // Check the JavaClass, which should be the same as the result as Class.forName(description).
  new TestCase( SECTION,
		"("+testval.description +").getClass().equals( " +
		"java.lang.Class.forName( '" + testval.classname +
		"' ) )",
		true,
		(javaval.javaclass).equals( testval.javaclass ) );
  // check the string value
  new TestCase(
    SECTION,
    "("+testval.description+") +''",
    testval.stringval,
    javaval.value +"" );
}
function JavaValue( value ) {
  this.type   = typeof value;
  this.value = value;
//  LC2 does not support the __proto__ property in Java objects
//  Object.prototype.toString will show its JavaScript wrapper object.
//    value.__proto__.getJSClass = Object.prototype.toString;
//    this.jsclass = value.getJSClass();
  this.javaclass = value.getClass();

  return this;
}
function TestValue( description, type, classname, stringval ) {
  this.description = description;
  this.type =  type;
  this.classname = classname;
  this.javaclass = java.lang.Class.forName( classname );
  this.stringval = stringval;

  return this;
}
