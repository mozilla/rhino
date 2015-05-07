/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.7.2.js';

/**
   File Name:          15.7.2.js
   ECMA Section:       15.7.2 The Number Constructor
   15.7.2.1
   15.7.2.2

   Description:        15.7.2 When Number is called as part of a new
   expression, it is a constructor:  it initializes
   the newly created object.

   15.7.2.1 The [[Prototype]] property of the newly
   constructed object is set to othe original Number
   prototype object, the one that is the initial value
   of Number.prototype(0).  The [[Class]] property is
   set to "Number".  The [[Value]] property of the
   newly constructed object is set to ToNumber(value)

   15.7.2.2 new Number().  same as in 15.7.2.1, except
   the [[Value]] property is set to +0.

   need to add more test cases.  see the gTestcases for
   TypeConversion ToNumber.

   Author:             christine@netscape.com
   Date:               29 september 1997
*/

var SECTION = "15.7.2";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "The Number Constructor";

writeHeaderToLog( SECTION + " "+ TITLE);

//  To verify that the object's prototype is the Number.prototype, check to see if the object's
//  constructor property is the same as Number.prototype.constructor.

new TestCase(SECTION, "(new Number()).constructor",      Number.prototype.constructor,   (new Number()).constructor );

new TestCase(SECTION, "typeof (new Number())",         "object",           typeof (new Number()) );
new TestCase(SECTION,  "(new Number()).valueOf()",     0,                   (new Number()).valueOf() );
new TestCase(SECTION,
	     "NUMB = new Number();NUMB.toString=Object.prototype.toString;NUMB.toString()",
	     "[object Number]",
	     eval("NUMB = new Number();NUMB.toString=Object.prototype.toString;NUMB.toString()") );

new TestCase(SECTION, "(new Number(0)).constructor",     Number.prototype.constructor,    (new Number(0)).constructor );
new TestCase(SECTION, "typeof (new Number(0))",         "object",           typeof (new Number(0)) );
new TestCase(SECTION,  "(new Number(0)).valueOf()",     0,                   (new Number(0)).valueOf() );
new TestCase(SECTION,
	     "NUMB = new Number(0);NUMB.toString=Object.prototype.toString;NUMB.toString()",
	     "[object Number]",
	     eval("NUMB = new Number(0);NUMB.toString=Object.prototype.toString;NUMB.toString()") );

new TestCase(SECTION, "(new Number(1)).constructor",     Number.prototype.constructor,    (new Number(1)).constructor );
new TestCase(SECTION, "typeof (new Number(1))",         "object",           typeof (new Number(1)) );
new TestCase(SECTION,  "(new Number(1)).valueOf()",     1,                   (new Number(1)).valueOf() );
new TestCase(SECTION,
	     "NUMB = new Number(1);NUMB.toString=Object.prototype.toString;NUMB.toString()",
	     "[object Number]",
	     eval("NUMB = new Number(1);NUMB.toString=Object.prototype.toString;NUMB.toString()") );

new TestCase(SECTION, "(new Number(-1)).constructor",     Number.prototype.constructor,    (new Number(-1)).constructor );
new TestCase(SECTION, "typeof (new Number(-1))",         "object",           typeof (new Number(-1)) );
new TestCase(SECTION,  "(new Number(-1)).valueOf()",     -1,                   (new Number(-1)).valueOf() );
new TestCase(SECTION,
	     "NUMB = new Number(-1);NUMB.toString=Object.prototype.toString;NUMB.toString()",
	     "[object Number]",
	     eval("NUMB = new Number(-1);NUMB.toString=Object.prototype.toString;NUMB.toString()") );

new TestCase(SECTION, "(new Number(Number.NaN)).constructor",     Number.prototype.constructor,    (new Number(Number.NaN)).constructor );
new TestCase(SECTION, "typeof (new Number(Number.NaN))",         "object",           typeof (new Number(Number.NaN)) );
new TestCase(SECTION,  "(new Number(Number.NaN)).valueOf()",     Number.NaN,                   (new Number(Number.NaN)).valueOf() );
new TestCase(SECTION,
	     "NUMB = new Number(Number.NaN);NUMB.toString=Object.prototype.toString;NUMB.toString()",
	     "[object Number]",
	     eval("NUMB = new Number(Number.NaN);NUMB.toString=Object.prototype.toString;NUMB.toString()") );

new TestCase(SECTION, "(new Number('string')).constructor",     Number.prototype.constructor,    (new Number('string')).constructor );
new TestCase(SECTION, "typeof (new Number('string'))",         "object",           typeof (new Number('string')) );
new TestCase(SECTION,  "(new Number('string')).valueOf()",     Number.NaN,                   (new Number('string')).valueOf() );
new TestCase(SECTION,
	     "NUMB = new Number('string');NUMB.toString=Object.prototype.toString;NUMB.toString()",
	     "[object Number]",
	     eval("NUMB = new Number('string');NUMB.toString=Object.prototype.toString;NUMB.toString()") );

new TestCase(SECTION, "(new Number(new String())).constructor",     Number.prototype.constructor,    (new Number(new String())).constructor );
new TestCase(SECTION, "typeof (new Number(new String()))",         "object",           typeof (new Number(new String())) );
new TestCase(SECTION,  "(new Number(new String())).valueOf()",     0,                   (new Number(new String())).valueOf() );
new TestCase(SECTION,
	     "NUMB = new Number(new String());NUMB.toString=Object.prototype.toString;NUMB.toString()",
	     "[object Number]",
	     eval("NUMB = new Number(new String());NUMB.toString=Object.prototype.toString;NUMB.toString()") );

new TestCase(SECTION, "(new Number('')).constructor",     Number.prototype.constructor,    (new Number('')).constructor );
new TestCase(SECTION, "typeof (new Number(''))",         "object",           typeof (new Number('')) );
new TestCase(SECTION,  "(new Number('')).valueOf()",     0,                   (new Number('')).valueOf() );
new TestCase(SECTION,
	     "NUMB = new Number('');NUMB.toString=Object.prototype.toString;NUMB.toString()",
	     "[object Number]",
	     eval("NUMB = new Number('');NUMB.toString=Object.prototype.toString;NUMB.toString()") );

new TestCase(SECTION, "(new Number(Number.POSITIVE_INFINITY)).constructor",     Number.prototype.constructor,    (new Number(Number.POSITIVE_INFINITY)).constructor );
new TestCase(SECTION, "typeof (new Number(Number.POSITIVE_INFINITY))",         "object",           typeof (new Number(Number.POSITIVE_INFINITY)) );
new TestCase(SECTION,  "(new Number(Number.POSITIVE_INFINITY)).valueOf()",     Number.POSITIVE_INFINITY,    (new Number(Number.POSITIVE_INFINITY)).valueOf() );
new TestCase(SECTION,
	     "NUMB = new Number(Number.POSITIVE_INFINITY);NUMB.toString=Object.prototype.toString;NUMB.toString()",
	     "[object Number]",
	     eval("NUMB = new Number(Number.POSITIVE_INFINITY);NUMB.toString=Object.prototype.toString;NUMB.toString()") );

new TestCase(SECTION, "(new Number(Number.NEGATIVE_INFINITY)).constructor",     Number.prototype.constructor,    (new Number(Number.NEGATIVE_INFINITY)).constructor );
new TestCase(SECTION, "typeof (new Number(Number.NEGATIVE_INFINITY))",         "object",           typeof (new Number(Number.NEGATIVE_INFINITY)) );
new TestCase(SECTION,  "(new Number(Number.NEGATIVE_INFINITY)).valueOf()",     Number.NEGATIVE_INFINITY,                   (new Number(Number.NEGATIVE_INFINITY)).valueOf() );
new TestCase(SECTION,
	     "NUMB = new Number(Number.NEGATIVE_INFINITY);NUMB.toString=Object.prototype.toString;NUMB.toString()",
	     "[object Number]",
	     eval("NUMB = new Number(Number.NEGATIVE_INFINITY);NUMB.toString=Object.prototype.toString;NUMB.toString()") );


new TestCase(SECTION, "(new Number()).constructor",     Number.prototype.constructor,    (new Number()).constructor );
new TestCase(SECTION, "typeof (new Number())",         "object",           typeof (new Number()) );
new TestCase(SECTION,  "(new Number()).valueOf()",     0,                   (new Number()).valueOf() );
new TestCase(SECTION,
	     "NUMB = new Number();NUMB.toString=Object.prototype.toString;NUMB.toString()",
	     "[object Number]",
	     eval("NUMB = new Number();NUMB.toString=Object.prototype.toString;NUMB.toString()") );

test();
