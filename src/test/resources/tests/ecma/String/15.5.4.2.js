/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.5.4.2.js';

/**
   File Name:          15.5.4.2.js
   ECMA Section:       15.5.4.2 String.prototype.toString

   Description:
   Author:             christine@netscape.com
   Date:               28 october 1997

*/
var SECTION = "15.5.4.2";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "String.prototype.tostring";

writeHeaderToLog( SECTION + " "+ TITLE);

new TestCase(   SECTION,
		"String.prototype.toString() == String.prototype.valueOf()",
		true,
		String.prototype.toString() == String.prototype.valueOf() );

new TestCase(   SECTION, "String.prototype.toString()",     "",     String.prototype.toString() );
new TestCase(   SECTION, "String.prototype.toString.length",    0,  String.prototype.toString.length );


new TestCase(   SECTION,
		"TESTSTRING = new String();TESTSTRING.valueOf() == TESTSTRING.toString()",
		true,
		eval("TESTSTRING = new String();TESTSTRING.valueOf() == TESTSTRING.toString()") );
new TestCase(   SECTION,
		"TESTSTRING = new String(true);TESTSTRING.valueOf() == TESTSTRING.toString()",
		true,
		eval("TESTSTRING = new String(true);TESTSTRING.valueOf() == TESTSTRING.toString()") );
new TestCase(   SECTION,
		"TESTSTRING = new String(false);TESTSTRING.valueOf() == TESTSTRING.toString()",
		true,
		eval("TESTSTRING = new String(false);TESTSTRING.valueOf() == TESTSTRING.toString()") );
new TestCase(   SECTION,
		"TESTSTRING = new String(Math.PI);TESTSTRING.valueOf() == TESTSTRING.toString()",
		true,
		eval("TESTSTRING = new String(Math.PI);TESTSTRING.valueOf() == TESTSTRING.toString()") );
new TestCase(   SECTION,
		"TESTSTRING = new String();TESTSTRING.valueOf() == TESTSTRING.toString()",
		true,
		eval("TESTSTRING = new String();TESTSTRING.valueOf() == TESTSTRING.toString()") );

test();
