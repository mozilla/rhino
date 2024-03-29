/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '8.6.2.1-1.js';

/**
   File Name:          8.6.2.1-1.js
   ECMA Section:       8.6.2.1 Get (Value)
   Description:

   When the [[Get]] method of O is called with property name P, the following
   steps are taken:

   1.  If O doesn't have a property with name P, go to step 4.
   2.  Get the value of the property.
   3.  Return Result(2).
   4.  If the [[Prototype]] of O is null, return undefined.
   5.  Call the [[Get]] method of [[Prototype]] with property name P.
   6.  Return Result(5).

   This tests [[Get]] (Value).

   Author:             christine@netscape.com
   Date:               12 november 1997
*/
var SECTION = "8.6.2.1-1";
var VERSION = "ECMA_1";
startTest();

writeHeaderToLog( SECTION + " [[Get]] (Value)");

new TestCase( SECTION,  "var OBJ = new MyValuelessObject(true); OBJ.valueOf()",     true,           eval("var OBJ = new MyValuelessObject(true); OBJ.valueOf()") );
//    new TestCase( SECTION,  "var OBJ = new MyProtoValuelessObject(true); OBJ + ''",     "undefined",    eval("var OBJ = new MyProtoValuelessObject(); OBJ + ''") );
new TestCase( SECTION,  "var OBJ = new MyProtolessObject(true); OBJ.valueOf()",     true,           eval("var OBJ = new MyProtolessObject(true); OBJ.valueOf()") );

new TestCase( SECTION,  "var OBJ = new MyValuelessObject(Number.POSITIVE_INFINITY); OBJ.valueOf()",     Number.POSITIVE_INFINITY,           eval("var OBJ = new MyValuelessObject(Number.POSITIVE_INFINITY); OBJ.valueOf()") );
//    new TestCase( SECTION,  "var OBJ = new MyProtoValuelessObject(Number.POSITIVE_INFINITY); OBJ + ''",     "undefined",                        eval("var OBJ = new MyProtoValuelessObject(); OBJ + ''") );
new TestCase( SECTION,  "var OBJ = new MyProtolessObject(Number.POSITIVE_INFINITY); OBJ.valueOf()",     Number.POSITIVE_INFINITY,           eval("var OBJ = new MyProtolessObject(Number.POSITIVE_INFINITY); OBJ.valueOf()") );

new TestCase( SECTION,  "var OBJ = new MyValuelessObject('string'); OBJ.valueOf()",     'string',           eval("var OBJ = new MyValuelessObject('string'); OBJ.valueOf()") );
//    new TestCase( SECTION,  "var OBJ = new MyProtoValuelessObject('string'); OJ + ''",     "undefined",      eval("var OBJ = new MyProtoValuelessObject(); OBJ + ''") );
new TestCase( SECTION,  "var OBJ = new MyProtolessObject('string'); OBJ.valueOf()",     'string',           eval("var OBJ = new MyProtolessObject('string'); OBJ.valueOf()") );

test();

function MyProtoValuelessObject(value) {
  this.valueOf = new Function ( "" );
  this.__proto__ = null;
}

function MyProtolessObject( value ) {
  this.valueOf = new Function( "return this.value" );
  this.__proto__ = null;
  this.value = value;
}
function MyValuelessObject(value) {
  this.__proto__ = new MyPrototypeObject(value);
}
function MyPrototypeObject(value) {
  this.valueOf = new Function( "return this.value;" );
  this.toString = new Function( "return (this.value + '');" );
  this.value = value;
}
