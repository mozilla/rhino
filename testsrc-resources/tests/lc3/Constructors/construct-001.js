/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'construct-001.js';

/**
 *  Verify that specific constructors can be invoked.
 *
 *
 */

var SECTION = "Explicit Constructor Invokation";
var VERSION = "JS1_4";
var TITLE   = "LiveConnect 3.0 JavaScript to Java Data Conversion";

startTest();

var DT = Packages.com.netscape.javascript.qa.liveconnect.DataTypeClass;

new TestCase(
  "dt = new DT()" +
  "dt.PUB_CONSTRUCTOR_ARG",
  new DT().CONSTRUCTOR_ARG_NONE,
  new DT().PUB_INT_CONSTRUCTOR_ARG );

new TestCase(
  "dt = new DT(5)" +
  "dt.PUB_CONSTRUCTOR_ARG",
  new DT(5).CONSTRUCTOR_ARG_DOUBLE,
  new DT(5).PUB_INT_CONSTRUCTOR_ARG );

new TestCase(
  "dt = new DT(\"true\")" +
  "dt.PUB_CONSTRUCTOR_ARG",
  new DT(true).CONSTRUCTOR_ARG_BOOLEAN,
  new DT(true).PUB_INT_CONSTRUCTOR_ARG );

// force type conversion

// convert boolean

new TestCase(
  "dt = new DT[\"(boolean)\"](true)" +
  "dt.PUB_CONSTRUCTOR_ARG",
  new DT("5").CONSTRUCTOR_ARG_BOOLEAN,
  new DT["(boolean)"](true).PUB_INT_CONSTRUCTOR_ARG );

new TestCase(
  "dt = new DT[\"(java.lang.Boolean)\"](true)" +
  "dt.PUB_CONSTRUCTOR_ARG",
  new DT().CONSTRUCTOR_ARG_BOOLEAN_OBJECT,
  new DT["(java.lang.Boolean)"](true).PUB_INT_CONSTRUCTOR_ARG );

new TestCase(
  "dt = new DT[\"(java.lang.Object)\"](true)" +
  "dt.PUB_CONSTRUCTOR_ARG",
  new DT("5").CONSTRUCTOR_ARG_OBJECT,
  new DT["(java.lang.Object)"](true).PUB_INT_CONSTRUCTOR_ARG );

new TestCase(
  "dt = new DT[\"(java.lang.String)\"](true)" +
  "dt.PUB_CONSTRUCTOR_ARG",
  new DT().CONSTRUCTOR_ARG_STRING,
  new DT["(java.lang.String)"](true).PUB_INT_CONSTRUCTOR_ARG );


// convert number

new TestCase(
  "dt = new DT[\"(double)\"](5)" +
  "dt.PUB_CONSTRUCTOR_ARG",
  new DT(5).CONSTRUCTOR_ARG_DOUBLE,
  new DT["(double)"]("5").PUB_INT_CONSTRUCTOR_ARG );

new TestCase(
  "dt = new DT[\"(java.lang.Double)\"](5)" +
  "dt.PUB_CONSTRUCTOR_ARG",
  new DT(5).CONSTRUCTOR_ARG_DOUBLE_OBJECT,
  new DT["(java.lang.Double)"](5).PUB_INT_CONSTRUCTOR_ARG );

new TestCase(
  "dt = new DT[\"(char)\"](5)" +
  "dt.PUB_CONSTRUCTOR_ARG",
  new DT().CONSTRUCTOR_ARG_CHAR,
  new DT["(char)"](5).PUB_INT_CONSTRUCTOR_ARG );

new TestCase(
  "dt = new DT[\"(java.lang.Object)\"](5)" +
  "dt.PUB_CONSTRUCTOR_ARG",
  new DT().CONSTRUCTOR_ARG_OBJECT,
  new DT["(java.lang.Object)"](5).PUB_INT_CONSTRUCTOR_ARG );

new TestCase(
  "dt = new DT[\"(java.lang.String)\"](5)" +
  "dt.PUB_CONSTRUCTOR_ARG",
  new DT().CONSTRUCTOR_ARG_STRING,
  new DT["(java.lang.String)"](5).PUB_INT_CONSTRUCTOR_ARG );

// convert string

new TestCase(
  "dt = new DT(\"5\")" +
  "dt.PUB_CONSTRUCTOR_ARG",
  new DT("5").CONSTRUCTOR_ARG_STRING,
  new DT("5").PUB_INT_CONSTRUCTOR_ARG );

new TestCase(
  "dt = new DT[\"(java.lang.String)\"](\"5\")" +
  "dt.PUB_CONSTRUCTOR_ARG",
  new DT("5").CONSTRUCTOR_ARG_STRING,
  new DT["(java.lang.String)"]("5").PUB_INT_CONSTRUCTOR_ARG );

new TestCase(
  "dt = new DT[\"(char)\"](\"J\")" +
  "dt.PUB_CONSTRUCTOR_ARG",
  new DT().CONSTRUCTOR_ARG_CHAR,
  new DT["(char)"]("J").PUB_INT_CONSTRUCTOR_ARG );

new TestCase(
  "dt = new DT[\"(double)\"](\"5\")" +
  "dt.PUB_CONSTRUCTOR_ARG",
  new DT("5").CONSTRUCTOR_ARG_DOUBLE,
  new DT["(double)"]("5").PUB_INT_CONSTRUCTOR_ARG );

new TestCase(
  "dt = new DT[\"(java.lang.Object)\"](\"hello\")" +
  "dt.PUB_CONSTRUCTOR_ARG",
  new DT("hello").CONSTRUCTOR_ARG_OBJECT,
  new DT["(java.lang.Object)"]("hello").PUB_INT_CONSTRUCTOR_ARG );

// convert java object

new TestCase(
  "dt = new DT[\"(java.lang.Object)\"](new java.lang.String(\"hello\")" +
  "dt.PUB_CONSTRUCTOR_ARG",
  new DT().CONSTRUCTOR_ARG_OBJECT,
  new DT["(java.lang.Object)"](new java.lang.String("hello")).PUB_INT_CONSTRUCTOR_ARG );

new TestCase(
  "dt = new DT[\"(java.lang.Object)\"](new java.lang.String(\"hello\")" +
  "dt.PUB_CONSTRUCTOR_ARG",
  new DT().CONSTRUCTOR_ARG_OBJECT,
  new DT["(java.lang.Object)"](new java.lang.String("hello")).PUB_INT_CONSTRUCTOR_ARG );

new TestCase(
  "dt = new DT[\"(double)\"](new java.lang.Double(5);" +
  "dt.PUB_CONSTRUCTOR_ARG",
  new DT().CONSTRUCTOR_ARG_DOUBLE,
  new DT["(double)"](new java.lang.Double(5)).PUB_INT_CONSTRUCTOR_ARG );

new TestCase(
  "dt = new DT[\"(char)\"](new java.lang.Double(5);" +
  "dt.PUB_CONSTRUCTOR_ARG",
  new DT().CONSTRUCTOR_ARG_CHAR,
  new DT["(char)"](new java.lang.Double(5)).PUB_INT_CONSTRUCTOR_ARG );

new TestCase(
  "dt = new DT[\"(java.lang.String)\"](new java.lang.Double(5);" +
  "dt.PUB_CONSTRUCTOR_ARG",
  new DT().CONSTRUCTOR_ARG_STRING,
  new DT["(java.lang.String)"](new java.lang.Double(5)).PUB_INT_CONSTRUCTOR_ARG );

new TestCase(
  "dt = new DT[\"(java.lang.Double)\"](new java.lang.Double(5);" +
  "dt.PUB_CONSTRUCTOR_ARG",
  new DT().CONSTRUCTOR_ARG_DOUBLE_OBJECT,
  new DT["(java.lang.Double)"](new java.lang.Double(5)).PUB_INT_CONSTRUCTOR_ARG );

new TestCase(
  "dt = new DT(new java.lang.Double(5);" +
  "dt.PUB_CONSTRUCTOR_ARG",
  new DT().CONSTRUCTOR_ARG_DOUBLE_OBJECT,
  new DT(new java.lang.Double(5)).PUB_INT_CONSTRUCTOR_ARG );

// java array

new TestCase(
  "dt = new DT[\"(java.lang.String)\"](new java.lang.String(\"hello\").getBytes())" +
  "dt.PUB_CONSTRUCTOR_ARG",
  new DT("hello").CONSTRUCTOR_ARG_STRING,
  new DT["(java.lang.String)"](new java.lang.String("hello").getBytes()).PUB_INT_CONSTRUCTOR_ARG );

new TestCase(
  "dt = new DT[\"(byte[])\"](new java.lang.String(\"hello\").getBytes())" +
  "dt.PUB_CONSTRUCTOR_ARG",
  new DT("hello").CONSTRUCTOR_ARG_BYTE_ARRAY,
  new DT["(byte[])"](new java.lang.String("hello").getBytes()).PUB_INT_CONSTRUCTOR_ARG );

test();
