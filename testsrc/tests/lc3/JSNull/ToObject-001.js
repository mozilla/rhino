/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'ToObject-001.js';

/* -*- Mode: java; tab-width: 8 -*-
 * Copyright (C) 1997, 1998 Netscape Communications Corporation,
 * All Rights Reserved.
 */
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
var SECTION = "null";
var VERSION = "1_4";
var TITLE   = "LiveConnect 3.0 JavaScript to Java Data Type Conversion " +
  SECTION;
startTest();

var dt = new DT();

var a = new Array();
var i = 0;

// passing a JavaScript null to a method that expect a class or interface
// converts null to a java null.  passing null to methods that expect
// number types convert null to 0.  passing null to methods that expect
// a boolean convert null to false.

a[i++] = new TestObject(
  "dt.setBooleanObject( null )",
  "dt.PUB_BOOLEAN_OBJECT",
  "dt.getBooleanObject()",
  "typeof dt.getBooleanObject()",
  null,
  "object" );

a[i++] = new TestObject(
  "dt.setDoubleObject( null )",
  "dt.PUB_BOOLEAN_OBJECT",
  "dt.getDoubleObject()",
  "typeof dt.getDoubleObject()",
  null,
  "object" );

a[i++] = new TestObject(
  "dt.setStringObject( null )",
  "dt.PUB_STRING",
  "dt.getStringObject()",
  "typeof dt.getStringObject()",
  null,
  "object" );

for ( i = 0; i < a.length; i++ ) {
  new TestCase(
    a[i].description +"; "+ a[i].javaFieldName,
    a[i].jsValue,
    a[i].javaFieldValue );
/*
  new TestCase(
  a[i].description +"; " + a[i].javaMethodName,
  a[i].jsValue,
  a[i].javaMethodValue );
*/
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
//    this.javaMethodName = javaMethod;
//    this.javaMethodValue = eval( javaMethod );
  this.javaTypeName = javaType,
    this.javaTypeValue = typeof this.javaFieldValue;

  this.jsValue   = jsValue;
  this.jsType      = jsType;
}
