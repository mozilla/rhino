/* -*- Mode: java; tab-width: 8 -*-
 * Copyright (C) 1997, 1998 Netscape Communications Corporation,
 * All Rights Reserved.
 */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'ToArray-002-n.js';

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
var SECTION = "JavaArray to Object[]";
var VERSION = "1_4";
var TITLE   = "LiveConnect 3.0 JavaScript to Java Data Type Conversion " +
  SECTION;
startTest();

var dt = new DT();

var a = new Array();
var i = 0;

// if the array is not an instance of the correct type, create a runtime
// error.

// Vector.copyInto expects an object array

DESCRIPTION = "var vector = new java.util.Vector(); "+
  "vector.addElement( \"a\" ); vector.addElement( \"b\" ); "+
  "vector.copyInto( DT.PUB_STATIC_ARRAY_CHAR )";
EXPECTED = "error";

a[i++] = new TestObject(
  "var vector = new java.util.Vector(); "+
  "vector.addElement( \"a\" ); vector.addElement( \"b\" ); "+
  "vector.copyInto( DT.PUB_STATIC_ARRAY_CHAR )",
  "DT.PUB_STATIC_ARRAY_OBJECT[0] +''",
  "DT.staticGetObjectArray()[0] +''",
  "typeof DT.staticGetObjectArray()[0]",
  "error",
  "error" );

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
    this.javaTypeValue = eval(javaType);

  this.jsValue   = jsValue;
  this.jsType      = jsType;
}
