/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'object-001.js';

/**
   File Name:      object-001.js
   Description:

   Given a JavaObject, calling the getClass() method of java.lang.Object
   should return the java.lang.Class of that object.

   To test this:

   1.  Create a JavaObject by instantiating a new object OR call
   a java method that returns a JavaObject.

   2.  Call getClass() on that object. Compare it to the result of
   java.lang.Class.forName( "<classname>" ).

   3.  Also compare the result of getClass() to the literal classname

   Note:  this test does not use the LiveConnect getClass function, which
   currently is not available.

   @author     christine@netscape.com
   @version    1.00
*/
var SECTION = "LiveConnect Objects";
var VERSION = "1_3";
var TITLE   = "Getting the Class of JavaObjects";

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

java_array[i] = new JavaValue(  new java.awt.Rectangle(1,2,3,4)  );
test_array[i] = new TestValue(  "new java.awt.Rectangle(1,2,3,4)", "java.awt.Rectangle" );
i++;

java_array[i] = new JavaValue(  new java.io.PrintStream( java.lang.System.out ) );
test_array[i] = new TestValue(  "new java.io.PrintStream(java.lang.System.out)", "java.io.PrintStream" );
i++;

java_array[i] = new JavaValue(  new java.lang.String("hello")  );
test_array[i] = new TestValue(  "new java.lang.String('hello')", "java.lang.String" );
i++;

java_array[i] = new JavaValue(  new java.net.URL("http://home.netscape.com/")  );
test_array[i] = new TestValue(  "new java.net.URL('http://home.netscape.com')", "java.net.URL" );
i++;

/*
  java_array[i] = new JavaValue(  java.rmi.RMISecurityManager  );
  test_array[i] = new TestValue(  "java.rmi.RMISecurityManager" );
  i++;
  java_array[i] = new JavaValue(  java.text.DateFormat  );
  test_array[i] = new TestValue(  "java.text.DateFormat" );
  i++;
*/
java_array[i] = new JavaValue(  new java.util.Vector()  );
test_array[i] = new TestValue(  "new java.util.Vector()", "java.util.Vector" );
i++;

/*
  java_array[i] = new JavaValue(  new Packages.com.netscape.javascript.Context()  );
  test_array[i] = new TestValue(  "new Packages.com.netscape.javascript.Context()", "com.netscape.javascript.Context" );
  i++;

  java_array[i] = new JavaValue(  Packages.com.netscape.javascript.examples.Shell  );
  test_array[i] = new TestValue(  "Packages.com.netscape.javascript.examples.Shell" );
  i++;
*/

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
testval.jsclass,
javaval.jsclass );
*/
  // Check the JavaClass, which should be the same as the result as Class.forName(description).
  new TestCase( SECTION,
		testval.description +".getClass().equals( " +
		"java.lang.Class.forName( '" + testval.classname +
		"' ) )",
		true,
		javaval.javaclass.equals( testval.javaclass ) );
}
function JavaValue( value ) {
  this.type   = typeof value;
//  LC2 does not support the __proto__ property in Java objects
  // Object.prototype.toString will show its JavaScript wrapper object.
//    value.__proto__.getJSClass = Object.prototype.toString;
//    this.jsclass = value.getJSClass();
  this.javaclass = value.getClass();
  return this;
}
function TestValue( description, classname ) {
  this.description = description;
  this.classname = classname;
  this.type =  E_TYPE;
  this.jsclass = E_JSCLASS;
  this.javaclass = java.lang.Class.forName( classname );
  return this;
}
