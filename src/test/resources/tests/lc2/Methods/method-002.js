/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'method-002.js';

/**
   File Name:      method-002.js
   Description:

   Call a method of a JavaObject instance and verify that return value.
   This is covered more thouroughly in the type conversion test cases.

   @author     christine@netscape.com
   @version    1.00
*/
var SECTION = "LiveConnect Objects";
var VERSION = "1_3";
var TITLE   = "Invoking Java Methods";

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

// method returns an object

var rect = new java.awt.Rectangle(1,2,3,4);
var size = rect.getSize();

new TestCase(
  SECTION,
  "var size = (new java.awt.Rectangle(1,2,3,4)).getSize(); "+
  "size.getClass().equals(java.lang.Class.forName(\""+
  "java.awt.Dimension\"))",
  true,
  size.getClass().equals(java.lang.Class.forName("java.awt.Dimension")));

new TestCase(
  SECTION,
  "size.width",
  3,
  size.width );

new TestCase(
  SECTION,
  "size.height",
  4,
  size.height );

// method returns void
var r = rect.setSize(5,6);

new TestCase(
  SECTION,
  "var r = rect.setSize(5,6); r",
  void 0,
  r );

// method returns a string

var string = new java.lang.String( "     hello     " );
s = string.trim()

  new TestCase(
    SECTION,
    "var string = new java.lang.String(\"     hello     \"); "+
    "var s = string.trim(); s.getClass().equals("+
    "java.lang.Class.forName(\"java.lang.String\")",
    true,
    s.getClass().equals(java.lang.Class.forName("java.lang.String")) );

// method returns an int
new TestCase(
  SECTION,
  "s.length()",
  5,
  s.length() );

test();

function CompareValues( javaval, testval ) {
  //  Check type, which should be E_TYPE
  new TestCase( SECTION,
		"typeof (" + testval.description +" )",
		testval.type,
		javaval.type );

  //  Check JavaScript class, which should be E_JSCLASS
  new TestCase( SECTION,
		"(" + testval.description +" ).getJSClass()",
		testval.jsclass,
		javaval.jsclass );
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
  // Object.prototype.toString will show its JavaScript wrapper object.
  value.__proto__.getJSClass = Object.prototype.toString;
  this.jsclass = value.getJSClass();
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
