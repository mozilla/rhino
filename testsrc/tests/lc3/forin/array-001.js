/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'array-001.js';

/**
 *  Verify that for-in loops can be used with java objects.
 *
 *  Java array members should be enumerated in for... in loops.
 *
 *
 */
var SECTION = "array-001";
var VERSION = "1_4";
var TITLE   = "LiveConnect 3.0:  for ... in java objects";
SECTION;
startTest();

// we just need to know the names of all the expected enumerated
// properties.  we will get the values to the original objects.

// for arrays, we just need to know the length, since java arrays
// don't have any extra properties


var dt = new Packages.com.netscape.javascript.qa.liveconnect.DataTypeClass;

var a = new Array();

a[a.length] = new TestObject(
  new java.lang.String("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789").getBytes(),
  "new java.lang.String(\"ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789\").getBytes()",
  36 );

a[a.length] = new TestObject(
  dt.PUB_ARRAY_SHORT,
  "dt.PUB_ARRAY_SHORT",
  dt.PUB_ARRAY_SHORT.length );

a[a.length] = new TestObject(
  dt.PUB_ARRAY_LONG,
  "dt.PUB_ARRAY_LONG",
  dt.PUB_ARRAY_LONG.length );

a[a.length] = new TestObject(
  dt.PUB_ARRAY_DOUBLE,
  "dt.PUB_ARRAY_DOUBLE",
  dt.PUB_ARRAY_DOUBLE.length );

a[a.length] = new TestObject(
  dt.PUB_ARRAY_BYTE,
  "dt.PUB_ARRAY_BYTE",
  dt.PUB_ARRAY_BYTE.length );

a[a.length] = new TestObject(
  dt.PUB_ARRAY_CHAR,
  "dt.PUB_ARRAY_CHAR",
  dt.PUB_ARRAY_CHAR.length );

a[a.length] = new TestObject(
  dt.PUB_ARRAY_OBJECT,
  "dt.PUB_ARRAY_OBJECT",
  dt.PUB_ARRAY_OBJECT.length );

for ( var i = 0; i < a.length; i++ ) {
  // check the number of properties of the enumerated object
  new TestCase(
    a[i].description +"; length",
    a[i].items,
    a[i].enumedArray.pCount );

  for ( var arrayItem = 0; arrayItem < a[i].items; arrayItem++ ) {
    new TestCase(
      "["+arrayItem+"]",
      a[i].javaArray[arrayItem],
      a[i].enumedArray[arrayItem] );
  }
}

test();

function TestObject( arr, descr, len ) {
  this.javaArray = arr;
  this.description = descr;
  this.items    = len;
  this.enumedArray = new enumObject(arr);
}

function enumObject( o ) {
  this.pCount = 0;
  for ( var p in o ) {
    this.pCount++;
    if ( !isNaN(p) ) {
      eval( "this["+p+"] = o["+p+"]" );
    } else {
      eval( "this." + p + " = o["+ p+"]" );
    }
  }
}

