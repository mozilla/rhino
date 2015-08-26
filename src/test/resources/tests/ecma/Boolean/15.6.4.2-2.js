/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.6.4.2-2.js';

/**
   File Name:          15.6.4.2-2.js
   ECMA Section:       15.6.4.2 Boolean.prototype.toString()
   Description:        Returns this boolean value.

   The toString function is not generic; it generates
   a runtime error if its this value is not a Boolean
   object.  Therefore it cannot be transferred to other
   kinds of objects for use as a method.

   Author:             christine@netscape.com
   Date:               june 27, 1997
*/

var SECTION = "15.6.4.2-2";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "Boolean.prototype.toString()"
  writeHeaderToLog( SECTION + TITLE );

new TestCase(   SECTION,
		"tostr=Boolean.prototype.toString; x=new Boolean(); x.toString=tostr;x.toString()",
		"false",
		eval("tostr=Boolean.prototype.toString; x=new Boolean(); x.toString=tostr;x.toString()") );
new TestCase(   SECTION,
		"tostr=Boolean.prototype.toString; x=new Boolean(true); x.toString=tostr; x.toString()",
		"true",
		eval("tostr=Boolean.prototype.toString; x=new Boolean(true); x.toString=tostr; x.toString()") );
new TestCase( SECTION,
	      "tostr=Boolean.prototype.toString; x=new Boolean(false); x.toString=tostr;x.toString()",
	      "false",
	      eval("tostr=Boolean.prototype.toString; x=new Boolean(); x.toString=tostr;x.toString()") );
test();
