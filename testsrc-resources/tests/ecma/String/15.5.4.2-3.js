/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.5.4.2-3.js';

/**
   File Name:          15.5.4.2-3.js
   ECMA Section:       15.5.4.2 String.prototype.toString()

   Description:        Returns this string value.  Note that, for a String
   object, the toString() method happens to return the same
   thing as the valueOf() method.

   The toString function is not generic; it generates a
   runtime error if its this value is not a String object.
   Therefore it connot be transferred to the other kinds of
   objects for use as a method.

   Author:             christine@netscape.com
   Date:               1 october 1997
*/


var SECTION = "15.5.4.2-3";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "String.prototype.toString";

writeHeaderToLog( SECTION + " "+ TITLE);

new TestCase( SECTION,
	      "var tostr=String.prototype.toString; astring=new String(); astring.toString = tostr; astring.toString()",
	      "",
	      eval("var tostr=String.prototype.toString; astring=new String(); astring.toString = tostr; astring.toString()") );
new TestCase( SECTION,
	      "var tostr=String.prototype.toString; astring=new String(0); astring.toString = tostr; astring.toString()",
	      "0",
	      eval("var tostr=String.prototype.toString; astring=new String(0); astring.toString = tostr; astring.toString()") );
new TestCase( SECTION,
	      "var tostr=String.prototype.toString; astring=new String('hello'); astring.toString = tostr; astring.toString()",
	      "hello",
	      eval("var tostr=String.prototype.toString; astring=new String('hello'); astring.toString = tostr; astring.toString()") );
new TestCase( SECTION,
	      "var tostr=String.prototype.toString; astring=new String(''); astring.toString = tostr; astring.toString()",
	      "",
	      eval("var tostr=String.prototype.toString; astring=new String(''); astring.toString = tostr; astring.toString()") );

test();
