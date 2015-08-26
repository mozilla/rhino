/* -*- Mode: java; tab-width: 8 -*-
 * Copyright (C) 1997, 1998 Netscape Communications Corporation,
 * All Rights Reserved.
 */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'ToDouble-002.js';

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
var SECTION = "number conversion";
var VERSION = "1_4";
var TITLE   = "LiveConnect 3.0 JavaScript to Java Data Type Conversion " +
  SECTION;
startTest();

var dt = new DT();

var a = new Array();
var i = 0;

// passing a number to a java method that expects a Double object should
// transfer the exact value to java with no rounding or loss of magnitude
// or sign.

// Special cases:  0, -0, Infinity, -Infinity, and NaN

a[i++] = new TestObject(
  "dt.setDoubleObject( 0 )",
  "dt.PUB_DOUBLE_OBJECT.doubleValue()",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  0,
  java.lang.Class.forName("java.lang.Double") );

a[i++] = new TestObject(
  "dt.setDoubleObject( -0 )",
  "Infinity / dt.PUB_DOUBLE_OBJECT",
  "Infinity / dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  -Infinity,
  java.lang.Class.forName("java.lang.Double") );

a[i++] = new TestObject(
  "dt.setDoubleObject( Infinity )",
  "dt.PUB_DOUBLE_OBJECT.doubleValue() ",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  Infinity,
  java.lang.Class.forName("java.lang.Double") );

a[i++] = new TestObject(
  "dt.setDoubleObject( -Infinity )",
  "dt.PUB_DOUBLE_OBJECT.doubleValue()",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  -Infinity,
  java.lang.Class.forName("java.lang.Double") );

a[i++] = new TestObject(
  "dt.setDoubleObject( NaN )",
  "dt.PUB_DOUBLE_OBJECT.doubleValue()",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  NaN,
  java.lang.Class.forName("java.lang.Double") );

// test cases from waldemar

a[i++] = new TestObject(
  "dt.setDoubleObject(077777777777777777)",
  "dt.PUB_DOUBLE_OBJECT.doubleValue()",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  2251799813685247,
  java.lang.Class.forName("java.lang.Double") );


a[i++] = new TestObject(
  "dt.setDoubleObject(077777777777777776)",
  "dt.PUB_DOUBLE_OBJECT.doubleValue()",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  2251799813685246,
  java.lang.Class.forName("java.lang.Double") );


a[i++] = new TestObject(
  "dt.setDoubleObject(0x1fffffffffffff)",
  "dt.PUB_DOUBLE_OBJECT.doubleValue()",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  9007199254740991,
  java.lang.Class.forName("java.lang.Double") );

a[i++] = new TestObject(
  "dt.setDoubleObject(0x20000000000000)",
  "dt.PUB_DOUBLE_OBJECT.doubleValue()",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  9007199254740992,
  java.lang.Class.forName("java.lang.Double") );

a[i++] = new TestObject(
  "dt.setDoubleObject(0x20123456789abc)",
  "dt.PUB_DOUBLE_OBJECT.doubleValue()",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  9027215253084860,
  java.lang.Class.forName("java.lang.Double") );

a[i++] = new TestObject(
  "dt.setDoubleObject(0x20123456789abd)",
  "dt.PUB_DOUBLE_OBJECT.doubleValue()",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  9027215253084860,
  java.lang.Class.forName("java.lang.Double") );

a[i++] = new TestObject(
  "dt.setDoubleObject(0x20123456789abe)",
  "dt.PUB_DOUBLE_OBJECT.doubleValue()",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  9027215253084862,
  java.lang.Class.forName("java.lang.Double") );

a[i++] = new TestObject(
  "dt.setDoubleObject(0x20123456789abf)",
  "dt.PUB_DOUBLE_OBJECT.doubleValue()",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  9027215253084864,
  java.lang.Class.forName("java.lang.Double") );

a[i++] = new TestObject(
  "dt.setDoubleObject(0x1000000000000080)",
  "dt.PUB_DOUBLE_OBJECT.doubleValue()",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  1152921504606847000,
  java.lang.Class.forName("java.lang.Double") );

a[i++] = new TestObject(
  "dt.setDoubleObject(0x1000000000000081)",
  "dt.PUB_DOUBLE_OBJECT.doubleValue()",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  1152921504606847200,
  java.lang.Class.forName("java.lang.Double") );

a[i++] = new TestObject(
  "dt.setDoubleObject(0x1000000000000100)",
  "dt.PUB_DOUBLE_OBJECT.doubleValue()",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  1152921504606847200,
  java.lang.Class.forName("java.lang.Double") );

a[i++] = new TestObject(
  "dt.setDoubleObject(0x100000000000017f)",
  "dt.PUB_DOUBLE_OBJECT.doubleValue()",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  1152921504606847200,
  java.lang.Class.forName("java.lang.Double") );

a[i++] = new TestObject(
  "dt.setDoubleObject(0x1000000000000180)",
  "dt.PUB_DOUBLE_OBJECT.doubleValue()",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  1152921504606847500,
  java.lang.Class.forName("java.lang.Double") );

a[i++] = new TestObject(
  "dt.setDoubleObject(0x1000000000000181)",
  "dt.PUB_DOUBLE_OBJECT.doubleValue()",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  1152921504606847500,
  java.lang.Class.forName("java.lang.Double") );

a[i++] = new TestObject(
  "dt.setDoubleObject(1.7976931348623157E+308)",
  "dt.PUB_DOUBLE_OBJECT.doubleValue()",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  1.7976931348623157e+308,
  java.lang.Class.forName("java.lang.Double") );

a[i++] = new TestObject(
  "dt.setDoubleObject(1.7976931348623158e+308)",
  "dt.PUB_DOUBLE_OBJECT.doubleValue()",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  1.7976931348623157e+308,
  java.lang.Class.forName("java.lang.Double") );

a[i++] = new TestObject(
  "dt.setDoubleObject(1.7976931348623159e+308)",
  "dt.PUB_DOUBLE_OBJECT.doubleValue()",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  Infinity,
  java.lang.Class.forName("java.lang.Double") );

a[i++] = new TestObject(
  "dt.setDoubleObject(-1.7976931348623157E+308)",
  "dt.PUB_DOUBLE_OBJECT.doubleValue()",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  -1.7976931348623157e+308,
  java.lang.Class.forName("java.lang.Double") );

a[i++] = new TestObject(
  "dt.setDoubleObject(-1.7976931348623158e+308)",
  "dt.PUB_DOUBLE_OBJECT.doubleValue()",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  -1.7976931348623157e+308,
  java.lang.Class.forName("java.lang.Double") );

a[i++] = new TestObject(
  "dt.setDoubleObject(-1.7976931348623159e+308)",
  "dt.PUB_DOUBLE_OBJECT.doubleValue()",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  -Infinity,
  java.lang.Class.forName("java.lang.Double") );

a[i++] = new TestObject(
  "dt.setDoubleObject(1e-2000)",
  "dt.PUB_DOUBLE_OBJECT.doubleValue()",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  0,
  java.lang.Class.forName("java.lang.Double") );

a[i++] = new TestObject(
  "dt.setDoubleObject(1e2000)",
  "dt.PUB_DOUBLE_OBJECT.doubleValue()",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  1e2000,
  java.lang.Class.forName("java.lang.Double") );

a[i++] = new TestObject(
  "dt.setDoubleObject(-1e2000)",
  "dt.PUB_DOUBLE_OBJECT.doubleValue()",
  "dt.getDoubleObject().doubleValue()",
  "dt.getDoubleObject().getClass()",
  -1e2000,
  java.lang.Class.forName("java.lang.Double") );

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
