/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'object-001.js';

/**
 *  java array objects "inherit" JS string methods.  verify that byte arrays
 *  can inherit JavaScript Array object methods join, reverse, sort and valueOf
 *
 */
var SECTION = "java array object inheritance JavaScript Array methods";
var VERSION = "1_4";
var TITLE   = "LiveConnect 3.0 " + SECTION;

startTest();

dt = new Packages.com.netscape.javascript.qa.liveconnect.DataTypeClass();

obArray = dt.PUB_ARRAY_OBJECT;

// check string value

new TestCase(
  "dt = new Packages.com.netscape.javascript.qa.liveconnect.DataTypeClass(); "+
  "obArray = dt.PUB_ARRAY_OBJECT" +
  "obArray.join() +''",
  join(obArray),
  obArray.join() );

// check type of object returned by method

new TestCase(
  "typeof obArray.reverse().join()",
  reverse(obArray),
  obArray.reverse().join() );

new TestCase(
  "obArray.reverse().getClass().getName() +''",
  "[Ljava.lang.Object;",
  obArray.reverse().getClass().getName() +'');

test();

function join( a ) {
  for ( var i = 0, s = ""; i < a.length; i++ ) {
    s += a[i].toString() + ( i + 1 < a.length ? "," : "" );
  }
  return s;
}
function reverse( a ) {
  for ( var i = a.length -1, s = ""; i >= 0; i-- ) {
    s += a[i].toString() + ( i> 0 ? "," : "" );
  }
  return s;
}
