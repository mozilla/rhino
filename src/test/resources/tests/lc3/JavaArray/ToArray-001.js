/* -*- Mode: java; tab-width: 8 -*-
 * Copyright (C) 1997, 1998 Netscape Communications Corporation,
 * All Rights Reserved.
 */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'ToArray-001.js';

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

// pass a byte array to a method that expects a string

a[i++] = new TestObject (
  "dt.setStringObject(java.lang.String(new java.lang.String(\"hello\").getBytes()))",
  "dt.PUB_STRING +''",
  "dt.getStringObject() +''",
  "typeof dt.getStringObject()",
  "hello",
  "object" );

// pass a char array to a method that expects a string

a[i++] = new TestObject (
  "dt.setStringObject(java.lang.String(new java.lang.String(\"goodbye\").toCharArray()))",
  "dt.PUB_STRING +''",
  "dt.getStringObject() +''",
  "typeof dt.getStringObject()",
  "goodbye",
  "object" );

a[i++] = new TestObject (
  "dt.setStringObject(java.lang.String(new java.lang.String(\"goodbye\").toCharArray()))",
  "dt.PUB_STRING +''",
  "dt.getStringObject() +''",
  "typeof dt.getStringObject()",
  "goodbye",
  "object" );

// Vector.copyInto expects an object array

a[i++] = new TestObject(
  "var vector = new java.util.Vector(); "+
  "vector.addElement( \"a\" ); vector.addElement( \"b\" ); "+
  "vector.copyInto( DT.PUB_STATIC_ARRAY_OBJECT )",
  "DT.PUB_STATIC_ARRAY_OBJECT[0] +''",
  "DT.staticGetObjectArray()[0] +''",
  "typeof DT.staticGetObjectArray()[0]",
  "a",
  "object" );

a[i++] = new TestObject(
  "var vector = new java.util.Vector(); "+
  "vector.addElement( \"a\" ); vector.addElement( 3 ); "+
  "vector.copyInto( DT.PUB_STATIC_ARRAY_OBJECT )",
  "DT.PUB_STATIC_ARRAY_OBJECT[1] +''",
  "DT.staticGetObjectArray()[1] +''",
  "DT.staticGetObjectArray()[1].getClass().getName() +''",
  "3.0",
  "java.lang.Double" );

// byte array

var random = Math.random() +"";

for ( counter = 0; counter < random.length; counter ++ ) {
  a[i++] = new TestObject (
    "dt.setByteArray(java.lang.String(\""+random+"\").getBytes());",
    "dt.PUB_STATIC_ARRAY_BYTE["+counter+"]",
    "dt.getByteArray()["+counter+"]",
    "typeof dt.getByteArray()["+counter+"]",
    random[counter].charCodeAt(0),
    "number" );
}

// char array

random = Math.random() +"";

for ( counter = 0; counter < random.length; counter ++ ) {
  a[i++] = new TestObject (
    "dt.setCharArray(java.lang.String(\""+random+"\").toCharArray());",
    "dt.PUB_STATIC_ARRAY_CHAR["+counter+"]",
    "dt.getCharArray()["+counter+"]",
    "typeof dt.getCharArray()["+counter+"]",
    random[counter].charCodeAt(0),
    "number" );
}

// int array

random = ( Math.round(Math.random() * Math.pow(10,dt.PUB_ARRAY_INT.length))) +"";
for ( counter = 0; counter < random.length; counter++ ) {
  dt.PUB_ARRAY_INT[counter] = random[counter];
}
for ( counter = 0; counter < random.length; counter ++ ) {
  a[i++] = new TestObject (
    "dt.setIntArray(dt.PUB_ARRAY_INT)",
    "DT.PUB_STATIC_ARRAY_INT["+counter+"]",
    "dt.getIntArray()["+counter+"]",
    "typeof dt.getIntArray()["+counter+"]",
    Number(random[counter]),
    "number" );
}

// short array

random = ( Math.round(Math.random() * Math.pow(10,dt.PUB_ARRAY_SHORT.length))) +"";
for ( counter = 0; counter < random.length; counter++ ) {
  dt.PUB_ARRAY_SHORT[counter] = random[counter];
}
for ( counter = 0; counter < random.length; counter ++ ) {
  a[i++] = new TestObject (
    "dt.setShortArray(dt.PUB_ARRAY_SHORT)",
    "DT.PUB_STATIC_ARRAY_SHORT["+counter+"]",
    "dt.getShortArray()["+counter+"]",
    "typeof dt.getShortArray()["+counter+"]",
    Number(random[counter]),
    "number" );
}

// long array

random = ( Math.round(Math.random() * Math.pow(10,dt.PUB_ARRAY_LONG.length))) +"";
for ( counter = 0; counter < random.length; counter++ ) {
  dt.PUB_ARRAY_LONG[counter] = random[counter];
}
for ( counter = 0; counter < random.length; counter ++ ) {
  a[i++] = new TestObject (
    "dt.setLongArray(dt.PUB_ARRAY_LONG)",
    "DT.PUB_STATIC_ARRAY_LONG["+counter+"]",
    "dt.getLongArray()["+counter+"]",
    "typeof dt.getLongArray()["+counter+"]",
    Number(random[counter]),
    "number" );
}


// double array

random = ( Math.round(Math.random() * Math.pow(10,dt.PUB_ARRAY_DOUBLE.length))) +"";
for ( counter = 0; counter < random.length; counter++ ) {
  dt.PUB_ARRAY_DOUBLE[counter] = random[counter];
}
for ( counter = 0; counter < random.length; counter ++ ) {
  a[i++] = new TestObject (
    "dt.setDoubleArray(dt.PUB_ARRAY_DOUBLE)",
    "DT.PUB_STATIC_ARRAY_DOUBLE["+counter+"]",
    "dt.getDoubleArray()["+counter+"]",
    "typeof dt.getDoubleArray()["+counter+"]",
    Number(random[counter]),
    "number" );
}

// float array

random = ( Math.round(Math.random() * Math.pow(10,dt.PUB_ARRAY_FLOAT.length))) +"";
for ( counter = 0; counter < random.length; counter++ ) {
  dt.PUB_ARRAY_FLOAT[counter] = random[counter];
}
for ( counter = 0; counter < random.length; counter ++ ) {
  a[i++] = new TestObject (
    "dt.setFloatArray(dt.PUB_ARRAY_FLOAT)",
    "DT.PUB_STATIC_ARRAY_FLOAT["+counter+"]",
    "dt.getFloatArray()["+counter+"]",
    "typeof dt.getFloatArray()["+counter+"]",
    Number(random[counter]),
    "number" );
}

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
