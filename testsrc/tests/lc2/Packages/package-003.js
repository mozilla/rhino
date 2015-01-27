/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'package-003.js';

/**
   File Name:      package-003.js
   Description:

   Set the package name to a JavaScript variable, and attempt to access
   classes relative to the package name.

   @author     christine@netscape.com
   @version    1.00
*/
var SECTION = "LiveConnect Packages";
var VERSION = "1_3";
var TITLE   = "LiveConnect Packages";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

//  All packages are of the type "object"
var E_TYPE = "object";

//  The JavaScript [[Class]] property for all Packages is "[JavaPackage <packagename>]"
var E_JSCLASS = "[JavaPackage ";

//  Create arrays of actual results (java_array) and
//  expected results (test_array).

var java_array = new Array();
var test_array = new Array();

var i = 0;

var js = Packages.javax.javascript;

java_array[i] = new JavaValue(  js  );
test_array[i] = new TestValue(  "javax.javascript" );
i++;

var util = java.util;

java_array[i] = new JavaValue(  util  );
test_array[i] = new TestValue(  "java.util" );
i++;

for ( i = 0; i < java_array.length; i++ ) {
  CompareValues( java_array[i], test_array[i] );

}

var v = new util.Vector();

test();

function CompareValues( javaval, testval ) {
  //  Check typeof, which should be E_TYPE
  new TestCase( SECTION,
		"typeof (" + testval.description +")",
		testval.type,
		javaval.type );

  //  Check JavaScript class, which should be E_JSCLASS + the package name
  new TestCase( SECTION,
		"(" + testval.description +").getJSClass()",
		testval.jsclass,
		javaval.jsclass );

  //  Number( package ) is NaN
  new TestCase( SECTION,
		"Number (" + testval.description +")",
		NaN,
		Number( javaval.value ) );

  //  String( package ) is string value
  new TestCase( SECTION,
		"String (" + testval.description +")",
		testval.jsclass,
		String(javaval.value) );
/*
//  ( package ).toString() is string value
new TestCase( SECTION,
"(" + testval.description +").toString()",
testval.jsclass,
(javaval.value).toString() );
*/
   //  Boolean( package ) is true
   new TestCase( SECTION,
		 "Boolean (" + testval.description +")",
		 true,
		 Boolean( javaval.value ) );
   //  add 0 is name + "0"
   new TestCase( SECTION,
		 "(" + testval.description +") +0",
		 testval.jsclass +"0",
		 javaval.value + 0);
}
function JavaValue( value ) {
  this.value  = value;
  this.type   = typeof value;
  this.jsclass = value +""
    return this;
}
function TestValue( description ) {
  this.packagename = (description.substring(0, "Packages.".length) ==
		      "Packages.") ? description.substring("Packages.".length, description.length ) :
    description;

  this.description = description;
  this.type =  E_TYPE;
  this.jsclass = E_JSCLASS +  this.packagename +"]";
  return this;
}
