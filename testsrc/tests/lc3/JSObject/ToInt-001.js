/* -*- Mode: java; tab-width: 8 -*-
 * Copyright (C) 1997, 1998 Netscape Communications Corporation,
 * All Rights Reserved.
 */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'ToInt-001.js';

/**
 *  JavaScript to Java type conversion.
 *
 *  This test passes JavaScript number values to several Java methods
 *  that expect arguments of various types, and verifies that the value is
 *  converted to the correct value and type.
 *
 *  This tests instance methods, and not static methods.
 *
 *  Running these tests successfully requires you to have
 *  com.netscape.javascript.qa.liveconnect.DataTypeClass on your classpath.
 *
 *  Specification:  Method Overloading Proposal for Liveconnect 3.0
 *
 *  @author: christine@netscape.com
 *
 */
var SECTION = "JavaScript Object to int";
var VERSION = "1_4";
var TITLE   = "LiveConnect 3.0 JavaScript to Java Data Type Conversion " +
  SECTION;
startTest();

var dt = new DT();

var a = new Array();
var i = 0;

// 3.3.6.4 Other JavaScript Objects
// Passing a JavaScript object to a java method that that expects a int
// should:
// 1. Apply the ToPrimitive operator (ECMA 9.3) to the JavaScript object
// with hint Number
// 2. Convert Result(1) to Java numeric type using the rules in 3.3.3.

var bool = new Boolean(true);

a[i++] = new TestObject(
  "dt.setInteger( bool )",
  "dt.PUB_INT",
  "dt.getInteger()",
  "typeof dt.getInteger()",
  1,
  "number");

bool = new Boolean(false);

a[i++] = new TestObject(
  "dt.setInteger( bool )",
  "dt.PUB_INT",
  "dt.getInteger()",
  "typeof dt.getInteger()",
  0,
  "number");

var number = new Number(0);

a[i++] = new TestObject(
  "dt.setInteger( number )",
  "dt.PUB_INT",
  "dt.getInteger()",
  "typeof dt.getInteger()",
  0,
  "number");

var string  = new String("2147483647");

a[i++] = new TestObject(
  "dt.setInteger(string)",
  "dt.PUB_INT",
  "dt.getInteger()",
  "typeof dt.getInteger()",
  2147483647,
  "number");

var string  = new String("-2147483648");

a[i++] = new TestObject(
  "dt.setInteger(string)",
  "dt.PUB_INT",
  "dt.getInteger()",
  "typeof dt.getInteger()",
  -2147483648,
  "number");

var myobject = new MyObject( "5.5" );

a[i++] = new TestObject(
  "dt.setInteger( myobject )",
  "dt.PUB_INT",
  "dt.getInteger()",
  "typeof dt.getInteger()",
  5,
  "number");

myobject = new MyOtherObject( "-107.5");

a[i++] = new TestObject(
  "dt.setInteger( myobject )",
  "dt.PUB_INT",
  "dt.getInteger()",
  "typeof dt.getInteger()",
  -107,
  "number");

myobject = new AnotherObject( "6666");

a[i++] = new TestObject(
  "dt.setInteger( myobject )",
  "dt.PUB_INT",
  "dt.getInteger()",
  "typeof dt.getInteger()",
  6666,
  "number");

for ( i = 0; i < a.length; i++ ) {
  new TestCase(
    a[i].description +"; "+ a[i].javaFieldName,
    a[i].jsValue,
    a[i].javaFieldValue );

  new TestCase(
    a[i].description +"; " + a[i].javaMethodName,
    a[i].jsValue,
    a[i].javaMethodValue );

  new TestCase(
    a[i].javaTypeName,
    a[i].jsType,
    a[i].javaTypeValue );
}

test();

function MyObject( stringValue ) {
  this.stringValue = String(stringValue);
  this.toString = new Function( "return this.stringValue" );
}

function MyOtherObject( value ) {
  this.toString = null;
  this.value = value;
  this.valueOf = new Function( "return this.value" );
}

function AnotherObject( value ) {
  this.toString = new Function( "return new Number(666)" );
  this.value = value;
  this.valueOf = new Function( "return this.value" );
}

function TestObject( description, javaField, javaMethod, javaType,
		     jsValue, jsType )
{
  eval (description );

  this.description = description;
  this.javaFieldName = javaField;
  this.javaFieldValue = eval( javaField );
  this.javaMethodName = javaMethod;
  this.javaMethodValue = eval( javaMethod );
  this.javaTypeName = javaType,
    this.javaTypeValue = eval( javaType );

  this.jsValue   = jsValue;
  this.jsType      = jsType;
}
