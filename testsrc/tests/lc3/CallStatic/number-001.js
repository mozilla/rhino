/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'number-001.js';

/**
 *  The Java language allows static methods to be invoked using either the
 *  class name or a reference to an instance of the class, but previous
 *  versions of liveocnnect only allowed the former.
 *
 *  Verify that we can call static methods and get the value of static fields
 *  from an instance reference.
 *
 *  author: christine@netscape.com
 *
 *  date:  12/9/1998
 *
 */
var SECTION = "Call static methods from an instance";
var VERSION = "1_4";
var TITLE   = "LiveConnect 3.0 " +
  SECTION;
startTest();

var DT = Packages.com.netscape.javascript.qa.liveconnect.DataTypeClass;
var dt = new DT();

var a = new Array;
var i = 0;

a[i++] = new TestObject(
  "dt.staticSetByte( 99 )",
  "dt.PUB_STATIC_BYTE",
  "dt.staticGetByte()",
  "typeof dt.staticGetByte()",
  99,
  "number" );

a[i++] = new TestObject(
  "dt.staticSetChar( 45678 )",
  "dt.PUB_STATIC_CHAR",
  "dt.staticGetChar()",
  "typeof dt.staticGetChar()",
  45678,
  "number" );

a[i++] = new TestObject(
  "dt.staticSetShort( 32109 )",
  "dt.PUB_STATIC_SHORT",
  "dt.staticGetShort()",
  "typeof dt.staticGetShort()",
  32109,
  "number" );

a[i++] = new TestObject(
  "dt.staticSetInteger( 2109876543 )",
  "dt.PUB_STATIC_INT",
  "dt.staticGetInteger()",
  "typeof dt.staticGetInteger()",
  2109876543,
  "number" );

a[i++] = new TestObject(
  "dt.staticSetLong( 9012345678901234567 )",
  "dt.PUB_STATIC_LONG",
  "dt.staticGetLong()",
  "typeof dt.staticGetLong()",
  9012345678901234567,
  "number" );


a[i++] = new TestObject(
  "dt.staticSetDouble( java.lang.Double.MIN_VALUE )",
  "dt.PUB_STATIC_DOUBLE",
  "dt.staticGetDouble()",
  "typeof dt.staticGetDouble()",
  java.lang.Double.MIN_VALUE,
  "number" );

a[i++] = new TestObject(
  "dt.staticSetFloat( java.lang.Float.MIN_VALUE )",
  "dt.PUB_STATIC_FLOAT",
  "dt.staticGetFloat()",
  "typeof dt.staticGetFloat()",
  java.lang.Float.MIN_VALUE,
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
