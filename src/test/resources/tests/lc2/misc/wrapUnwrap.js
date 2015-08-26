/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'wrapUnwrap.js';

/**
   File Name:          wrapUnwrap.js
   Section:            LiveConnect
   Description:

   Tests wrapping and unwrapping objects.
   @author mikeang

*/
var SECTION = "wrapUnwrap.js";
var VERSION = "JS1_3";
var TITLE   = "LiveConnect";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

var hashtable = new java.util.Hashtable();
var sameHashtable = hashtable;

jsEquals(hashtable,sameHashtable);
javaEquals(hashtable,sameHashtable);

function returnString(theString) {
  return theString;
}
var someString = new java.lang.String("foo");
var sameString = returnString(someString);
jsEquals(someString,sameString);
javaEquals(someString,sameString);

var assignToProperty = new Object();
assignToProperty.assignedString = someString;
jsEquals(someString,assignToProperty.assignedString);
javaEquals(someString,assignToProperty.assignedString);

function laConstructor(a,b,c) {
  this.one = a;
  this.two = b;
  this.three = c;
}
var stack1 = new java.util.Stack();
var stack2 = new java.util.Stack();
var num = 28;
var constructed = new laConstructor(stack1, stack2, num);
javaEquals(stack1, constructed.one);
javaEquals(stack2, constructed.two);
jsEquals(num, constructed.three);

test();

function jsEquals(expectedResult, actualResult, message) {
  new TestCase( SECTION,
		expectedResult +" == "+actualResult,
		expectedResult,
		actualResult );
}

function javaEquals(expectedResult, actualResult, message) {
  new TestCase( SECTION,
		expectedResult +" == "+actualResult,
		expectedResult,
		actualResult );
}
