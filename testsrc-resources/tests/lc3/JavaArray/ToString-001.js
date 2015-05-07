/* -*- Mode: java; tab-width: 8 -*-
 * Copyright (C) 1997, 1998 Netscape Communications Corporation,
 * All Rights Reserved.
 */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'ToString-001.js';

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
var SECTION = "JavaArray to String";
var VERSION = "1_4";
var TITLE   = "LiveConnect 3.0 JavaScript to Java Data Type Conversion " +
  SECTION;
startTest();

var dt = new DT();

var a = new Array();
var i = 0;

// Passing a JavaArray to a method that expects a java.lang.String should
// call the unwrapped array's toString method and return the result as a
// new java.lang.String.

// this should return the byte array string representation, which includes
// the object's hash code

a[i++] = new TestObject (
  "dt.setStringObject( DT.staticGetByteArray() )",
  "dt.PUB_STRING +''",
  "dt.getStringObject() +''",
  "typeof dt.getStringObject() +''",
  DT.PUB_STATIC_ARRAY_BYTE +"",
  "string" );


a[i++] = new TestObject (
  "dt.setStringObject( DT.staticGetCharArray() )",
  "dt.PUB_STRING +''",
  "dt.getStringObject() +''",
  "typeof dt.getStringObject() +''",
  DT.PUB_STATIC_ARRAY_CHAR +"",
  "string" );

a[i++] = new TestObject (
  "dt.setStringObject( DT.staticGetShortArray() )",
  "dt.PUB_STRING +''",
  "dt.getStringObject() +''",
  "typeof dt.getStringObject() +''",
  DT.PUB_STATIC_ARRAY_SHORT +"",
  "string" );

a[i++] = new TestObject (
  "dt.setStringObject( DT.staticGetLongArray() )",
  "dt.PUB_STRING +''",
  "dt.getStringObject() +''",
  "typeof dt.getStringObject() +''",
  DT.PUB_STATIC_ARRAY_LONG +"",
  "string" );

a[i++] = new TestObject (
  "dt.setStringObject( DT.staticGetIntArray() )",
  "dt.PUB_STRING +''",
  "dt.getStringObject() +''",
  "typeof dt.getStringObject() +''",
  DT.PUB_STATIC_ARRAY_INT +"",
  "string" );

a[i++] = new TestObject (
  "dt.setStringObject( DT.staticGetFloatArray() )",
  "dt.PUB_STRING +''",
  "dt.getStringObject() +''",
  "typeof dt.getStringObject() +''",
  DT.PUB_STATIC_ARRAY_FLOAT +"",
  "string" );

a[i++] = new TestObject (
  "dt.setStringObject( DT.staticGetObjectArray() )",
  "dt.PUB_STRING +''",
  "dt.getStringObject() +''",
  "typeof dt.getStringObject() +''",
  DT.PUB_STATIC_ARRAY_OBJECT +"",
  "string" );

a[i++] = new TestObject (
  "dt.setStringObject(java.lang.String(new java.lang.String(\"hello\").getBytes()))",
  "dt.PUB_STRING +''",
  "dt.getStringObject() +''",
  "typeof dt.getStringObject() +''",
  "hello",
  "string" );

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
    this.javaTypeValue = typeof this.javaFieldValue;

  this.jsValue   = jsValue;
  this.jsType      = jsType;
}
