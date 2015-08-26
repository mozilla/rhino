/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.6.4-1.js';

/**
   File Name:          15.6.4-1.js
   ECMA Section:       15.6.4 Properties of the Boolean Prototype Object

   Description:
   The Boolean prototype object is itself a Boolean object (its [[Class]] is
   "Boolean") whose value is false.

   The value of the internal [[Prototype]] property of the Boolean prototype object
   is the Object prototype object (15.2.3.1).

   Author:             christine@netscape.com
   Date:               30 september 1997

*/


var VERSION = "ECMA_1"
  startTest();
var SECTION = "15.6.4-1";

writeHeaderToLog( SECTION + " Properties of the Boolean Prototype Object");

new TestCase( SECTION, "typeof Boolean.prototype == typeof( new Boolean )", true,          typeof Boolean.prototype == typeof( new Boolean ) );
new TestCase( SECTION, "typeof( Boolean.prototype )",              "object",               typeof(Boolean.prototype) );
new TestCase( SECTION,
	      "Boolean.prototype.toString = Object.prototype.toString; Boolean.prototype.toString()",
	      "[object Boolean]",
	      eval("Boolean.prototype.toString = Object.prototype.toString; Boolean.prototype.toString()") );
new TestCase( SECTION, "Boolean.prototype.valueOf()",               false,                  Boolean.prototype.valueOf() );

test();
