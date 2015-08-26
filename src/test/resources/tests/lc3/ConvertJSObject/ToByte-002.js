/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'ToByte-002.js';

/**
 *  Preferred Argument Conversion.
 *
 *  Passing a JavaScript boolean to a Java method should prefer to call
 *  a Java method of the same name that expects a Java boolean.
 *
 */
var SECTION = "Preferred argument conversion:  JavaScript Object to int";
var VERSION = "1_4";
var TITLE   = "LiveConnect 3.0 JavaScript to Java Data Type Conversion " +
  SECTION;
startTest();

var TEST_CLASS = new Packages.com.netscape.javascript.qa.lc3.jsobject.JSObject_010;

function MyFunction() {
  return "hello";
}
MyFunction.valueOf = new Function( "return 99" );

function MyOtherFunction() {
  return "goodbye";
}
MyOtherFunction.valueOf = null;
MyOtherFunction.toString = new Function( "return 99" );

function MyObject(value) {
  this.value = value;
  this.valueOf = new Function("return this.value");
}

function MyOtherObject(stringValue) {
  this.stringValue = String( stringValue );
  this.toString = new Function( "return this.stringValue" );
  this.valueOf = null;
}

function AnotherObject( value ) {
  this.value = value;
  this.valueOf = new Function( "return this.value" );
  this.toString = new Function( "return 666" );
}

// should pass MyFunction.valueOf() to ambiguous

new TestCase(
  "TEST_CLASS.ambiguous( MyFunction ) +''",
  "BYTE",
  TEST_CLASS.ambiguous( MyFunction )+'' );

// should pass MyOtherFunction.toString() to ambiguous

new TestCase(
  "TEST_CLASS.ambiguous( MyOtherFunction ) +''",
  "BYTE",
  TEST_CLASS.ambiguous( MyOtherFunction )+'' );

// should pass MyObject.valueOf() to ambiguous

new TestCase(
  "TEST_CLASS.ambiguous( new MyObject(123) ) +''",
  "BYTE",
  TEST_CLASS.ambiguous( new MyObject(123) )+'' );

// should pass MyOtherObject.toString() to ambiguous

new TestCase(
  "TEST_CLASS.ambiguous( new MyOtherObject(\"123\") ) +''",
  "BYTE",
  TEST_CLASS.ambiguous( new MyOtherObject("123") )+'' );

// should pass AnotherObject.valueOf() to ambiguous

new TestCase(
  "TEST_CLASS.ambiguous( new AnotherObject(\"123\") ) +''",
  "BYTE",
  TEST_CLASS.ambiguous( new AnotherObject("123") )+'' );

test();

