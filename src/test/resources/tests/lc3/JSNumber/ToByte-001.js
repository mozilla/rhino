/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'ToByte-001.js';

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
var SECTION = "number conversion to byte";
var VERSION = "1_4";
var TITLE   = "LiveConnect 3.0 JavaScript to Java Data Type Conversion " +
  SECTION;
startTest();

var dt = new DT();

var a = new Array();
var i = 0;

// passing a JS number to a method that expects a int:
// round JS number to integral value using round-to-negative-infinity mode
// numbers with a magnitude too large to be represented in the target integral
//  type result in a runtime error.
// Nan converts to 0.

// Special cases:  0, -0, Infinity, -Infinity, and NaN

a[i++] = new TestObject(
  "dt.setByte( 0 )",
  "dt.PUB_BYTE",
  "dt.getByte()",
  "typeof dt.getByte()",
  0,
  "number" );

// 0 can only have a negative sign for floats and doubles

a[i++] = new TestObject(
  "dt.setByte( -0 )",
  "Infinity / dt.PUB_BYTE",
  "Infinity / dt.getByte()",
  "typeof dt.getByte()",
  Infinity,
  "number" );

a[i++] = new TestObject(
  "dt.setByte( java.lang.Byte.MAX_VALUE )",
  "dt.PUB_BYTE",
  "dt.getByte()",
  "typeof dt.getByte()",
  java.lang.Byte.MAX_VALUE,
  "number" );

a[i++] = new TestObject(
  "dt.setByte( java.lang.Byte.MIN_VALUE )",
  "dt.PUB_BYTE",
  "dt.getByte()",
  "typeof dt.getByte()",
  java.lang.Byte.MIN_VALUE,
  "number" );

a[i++] = new TestObject(
  "dt.setByte( -java.lang.Byte.MAX_VALUE )",
  "dt.PUB_BYTE",
  "dt.getByte()",
  "typeof dt.getByte()",
  -java.lang.Byte.MAX_VALUE,
  "number" );

a[i++] = new TestObject(
  "dt.setByte(1e-2000)",
  "dt.PUB_BYTE",
  "dt.getByte()",
  "typeof dt.getByte()",
  0,
  "number" );

a[i++] = new TestObject(
  "dt.setByte(-1e-2000)",
  "dt.PUB_BYTE",
  "dt.getByte()",
  "typeof dt.getByte()",
  0,
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
    this.javaTypeValue = eval( javaType );

  this.jsValue   = jsValue;
  this.jsType      = jsType;
}
