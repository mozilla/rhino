/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'JavaObjectToInt-002-n.js';

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
var SECTION = "JavaObject to int";
var VERSION = "1_4";
var TITLE   = "LiveConnect 3.0 JavaScript to Java Data Type Conversion " +
  SECTION;
startTest();

var dt = new DT();

var a = new Array();
var i = 0;

// 1. If the object does not have a doubleValue() method, a runtime error
//    occurs.
// 2. Call the object's doubleValue() method
// 3. If result(2) is NaN, a runtime error occurs
// 3. Convert Result(2) to Java numeric type. Round JS number to integral
//    value using round-to-negative-infinity mode.
//    Numbers with a magnitude too large to be represented in the target
//    integral type should result in a runtime error.

DESCRIPTION = "dt.PUB_DOUBLE_REPRESENTATION = NaN ;"+
  "dt.setInteger( dt )";
EXPECTED = "error";

a[i++] = new TestObject (
  "dt.PUB_DOUBLE_REPRESENTATION = NaN ;"+
  "dt.setInteger( dt )",
  "dt.PUB_INTEGER",
  "dt.getInteger()",
  "typeof dt.getInteger()",
  "error",
  "number" );

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
