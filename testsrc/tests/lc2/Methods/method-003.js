/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'method-003.js';

/**
   File Name:      method-003.js
   Description:

   JavaMethod objects are of the type "function" since they implement the
   [[Call]] method.


   @author     christine@netscape.com
   @version    1.00
*/
var SECTION = "LiveConnect Objects";
var VERSION = "1_3";
var TITLE   = "Type and Class of Java Methods";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

//  All JavaMethods are of the type "function"
var E_TYPE = "function";

//  All JavaMethods [[Class]] property is Function
var E_JSCLASS = "[object Function]";

//  Create arrays of actual results (java_array) and
//  expected results (test_array).

var java_array = new Array();
var test_array = new Array();

var i = 0;

java_array[i] = new JavaValue(  java.lang.System.out.println  );
test_array[i] = new TestValue(  "java.lang.System.out.println" );
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

  //  Check JavaScript class, which should be E_JSCLASS
  new TestCase( SECTION,
		"(" + testval.description +" ).getJSClass()",
		testval.jsclass,
		javaval.jsclass );
}
function JavaValue( value ) {
  this.type   = typeof value;
  // Object.prototype.toString will show its JavaScript wrapper object.
  value.getJSClass = Object.prototype.toString;
  this.jsclass = value.getJSClass();
  return this;
}
function TestValue( description  ) {
  this.description = description;
  this.type =  E_TYPE;
  this.jsclass = E_JSCLASS;
  return this;
}
