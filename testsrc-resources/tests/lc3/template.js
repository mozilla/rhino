/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'template.js';

/**
 *  Template for LiveConnect 3.0 tests.
 *
 *
 */
var SECTION = "undefined conversion";
var VERSION = "1_4";
var TITLE   = "LiveConnect 3.0 JavaScript to Java Data Type Conversion " +
  SECTION;
startTest();

// typeof all resulting objects is "object";
var E_TYPE = "object";

// JS class of all resulting objects is "JavaObject";
var E_JSCLASS = "[object JavaObject]";

var a = new Array();
var i = 0;

a[i++] = new TestObject( "java.lang.Long.toString(0)",
			 java.lang.Long.toString(0), "0" );

a[i++] = new TestObject( "java.lang.Long.toString(NaN)",
			 java.lang.Long.toString(NaN), "0" );

a[i++] = new TestObject( "java.lang.Long.toString(5)",
			 java.lang.Long.toString(5), "5" );

a[i++] = new TestObject( "java.lang.Long.toString(9.9)",
			 java.lang.Long.toString(9.9), "9" );

a[i++] = new TestObject( "java.lang.Long.toString(-9.9)",
			 java.lang.Long.toString(-9.9), "-9" );

for ( var i = 0; i < a.length; i++ ) {

  // check typeof
  new TestCase(
    SECTION,
    "typeof (" + a[i].description +")",
    a[i].type,
    typeof a[i].javavalue );

  // check the number value of the object
  new TestCase(
    SECTION,
    "String(" + a[i].description +")",
    a[i].jsvalue,
    String( a[i].javavalue ) );
}

test();

function TestObject( description, javavalue, jsvalue ) {
  this.description = description;
  this.javavalue = javavalue;
  this.jsvalue = jsvalue;
  this.type = E_TYPE;
  return this;
}
