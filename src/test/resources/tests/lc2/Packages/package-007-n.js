/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'package-007-n.js';

/**
   File Name:      package-007-n.js
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

java.util[0] ="hi";
java.util["x"] = "bye";

new TestCase( SECTION,
	      "java.util[0] = \"hi\"; typeof java.util[0]",
	      "undefined",
	      typeof java.util[0] );

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
		javaval.getJSClass() );

  //  Number( package ) is NaN
  new TestCase( SECTION,
		"Number (" + testval.description +")",
		NaN,
		Number( javaval ) );

  //  String( package ) is string value
  new TestCase( SECTION,
		"String (" + testval.description +")",
		testval.jsclass,
		String(javaval) );
  //  ( package ).toString() is string value
  new TestCase( SECTION,
		"(" + testval.description +").toString()",
		testval.jsclass,
		(javaval).toString() );

  //  Boolean( package ) is true
  new TestCase( SECTION,
		"Boolean (" + testval.description +")",
		true,
		Boolean( javaval ) );
  //  add 0 is name + "0"
  new TestCase( SECTION,
		"(" + testval.description +") +0",
		testval.jsclass +"0",
		javaval + 0);
}
function JavaValue( value ) {
  this.value  = value;
  this.type   = typeof value;
  this.getJSClass = Object.prototype.toString;
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
