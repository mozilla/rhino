/* -*- Mode: java; tab-width: 8 -*-
 * Copyright (C) 1997, 1998 Netscape Communications Corporation,
 * All Rights Reserved.
 */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'ToJSObject-001.js';

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
var SECTION = "JavaClass to java.lang.Class";
var VERSION = "1_4";
var TITLE   = "LiveConnect 3.0 JavaScript to Java Data Type Conversion " +
  SECTION;

var BUGNUMBER ="335885";

startTest();

var jsoc = new
  Packages.com.netscape.javascript.qa.liveconnect.JSObjectConversion();

var a = new Array();
var i = 0;

// Pass a JavaClass to a method that expects a JSObject should wrap
// the object in a new instance of JSObject.

a[i++] = new TestObject (
  "var newClass = java.lang.Integer; "+
  "jsoc.setJSObject( newClass )",
  "jsoc.PUB_JSOBJECT",
  "jsoc.getJSObject()",
  "jsoc.getJSObject()",
  java.lang.Integer,
  "function" );


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
