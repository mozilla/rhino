/* -*- Mode: java; tab-width: 8 -*-
 * Copyright (C) 1997, 1998 Netscape Communications Corporation,
 * All Rights Reserved.
 */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'ToObject-001.js';

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
var SECTION = "JavaScript Object to java.lang.String";
var VERSION = "1_4";
var TITLE   = "LiveConnect 3.0 JavaScript to Java Data Type Conversion " +
  SECTION;
var BUGNUMBER="335882";

startTest();

var dt = new DT();

var a = new Array();
var i = 0;

// 3.3.6.4 Other JavaScript Objects
// Passing a JavaScript object to a java method that that expects a JSObject
// should wrap the JS object in a new instance of netscape.javascript.JSObject.
// HOwever, since we are running the test from JavaScript, getting the value
// back should return the unwrapped object.

var string  = new String("JavaScript String Value");

a[i++] = new TestObject(
  "dt.setObject(string)",
  "dt.PUB_OBJECT +''",
  "dt.getObject() +''",
  "dt.getObject().constructor",
  string +"",
  String);

var myobject = new MyObject( string );

a[i++] = new TestObject(
  "dt.setObject( myobject )",
  "dt.PUB_OBJECT",
  "dt.getObject()",
  "dt.getObject().constructor",
  myobject,
  MyObject);

var bool = new Boolean(true);

a[i++] = new TestObject(
  "dt.setObject( bool )",
  "dt.PUB_OBJECT",
  "dt.getObject()",
  "dt.getObject().constructor",
  bool,
  Boolean);

bool = new Boolean(false);

a[i++] = new TestObject(
  "dt.setObject( bool )",
  "dt.PUB_OBJECT",
  "dt.getObject()",
  "dt.getObject().constructor",
  bool,
  Boolean);

var object = new Object();

a[i++] = new TestObject(
  "dt.setObject( object)",
  "dt.PUB_OBJECT",
  "dt.getObject()",
  "dt.getObject().constructor",
  object,
  Object);

var number = new Number(0);

a[i++] = new TestObject(
  "dt.setObject( number )",
  "dt.PUB_OBJECT",
  "dt.getObject()",
  "dt.getObject().constructor",
  number,
  Number);

nan = new Number(NaN);

a[i++] = new TestObject(
  "dt.setObject( nan )",
  "dt.PUB_OBJECT",
  "dt.getObject()",
  "dt.getObject().constructor",
  nan,
  Number);

infinity = new Number(Infinity);

a[i++] = new TestObject(
  "dt.setObject( infinity )",
  "dt.PUB_OBJECT",
  "dt.getObject()",
  "dt.getObject().constructor",
  infinity,
  Number);

var neg_infinity = new Number(-Infinity);

a[i++] = new TestObject(
  "dt.setObject( neg_infinity )",
  "dt.PUB_OBJECT",
  "dt.getObject()",
  "dt.getObject().constructor",
  neg_infinity,
  Number);

var array = new Array(1,2,3)

  a[i++] = new TestObject(
    "dt.setObject(array)",
    "dt.PUB_OBJECT",
    "dt.getObject()",
    "dt.getObject().constructor",
    array,
    Array);


a[i++] = new TestObject(
  "dt.setObject( MyObject )",
  "dt.PUB_OBJECT",
  "dt.getObject()",
  "dt.getObject().constructor",
  MyObject,
  Function);

var THIS = this;

a[i++] = new TestObject(
  "dt.setObject( THIS )",
  "dt.PUB_OBJECT",
  "dt.getObject()",
  "dt.getObject().constructor",
  this,
  Object);

a[i++] = new TestObject(
  "dt.setObject( Math )",
  "dt.PUB_OBJECT",
  "dt.getObject()",
  "dt.getObject().constructor",
  Math,
  Object);

a[i++] = new TestObject(
  "dt.setObject( Function )",
  "dt.PUB_OBJECT",
  "dt.getObject()",
  "dt.getObject().constructor",
  Function,
  Function);

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
