/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'object-005.js';

/**
   File Name:      object-005.js
   Description:

   Call ToNumber, ToString, and use the addition operator to
   access the DefaultValue with hints Number, String, and no
   hint (respectively).

   @author     christine@netscape.com
   @version    1.00
*/
var SECTION = "LiveConnect Objects";
var VERSION = "1_3";
var TITLE   = "Getting the Class of JavaObjects";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

var a = new Array();
var i = 0;

// here's an object that should be converted to a number
a[i++] = new TestObject( "new java.lang.Integer(999)",
			 new java.lang.Integer(999), "Number", 999, "number" );

a[i++] = new TestObject(
  "new java.lang.Float(999.0)",
  new java.lang.Float(999.0),
  "Number",
  999,
  "number" );

a[i++] = new TestObject(
  "new java.lang.String(\"hi\")",
  new java.lang.String("hi"),
  "String",
  "hi",
  "string" );

a[i++] = new TestObject(
  "new java.lang.Integer(2134)",
  new java.lang.Integer(2134),
  "0 + ",
  "21340",
  "string" );

a[i++] = new TestObject(
  "new java.lang.Integer(666)",
  new java.lang.Integer(666),
  "Boolean",
  true,
  "boolean" );

for ( i = 0; i < a.length; i++ ) {
  CompareValues( a[i] );
}

test();

function CompareValues( t ) {
  new TestCase(
    SECTION,
    t.converter +"("+ t.description +")",
    t.expect,
    t.actual );

  new TestCase(
    SECTION,
    "typeof (" + t.converter +"( "+ t.description +" ) )",
    t.type,
    typeof t.actual );
}
function TestObject( description, javaobject, converter, expect, type ) {
  this.description = description;
  this.javavalue = javaobject
    this.converter = converter;
  this.expect = expect;
  this.type = type;

  switch ( converter ) {
  case( "Number" ) :  this.actual = Number( javaobject ); break;
  case( "String" ) :  this.actual = String( javaobject ); break;
  case( "Boolean") :  this.actual = Boolean(javaobject ); break;
  default:            this.actual = javaobject + 0;
  }
  return this;
}
