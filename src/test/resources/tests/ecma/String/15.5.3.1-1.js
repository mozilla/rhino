/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.5.3.1-1.js';

/**
   File Name:          15.5.3.1-1.js
   ECMA Section:       15.5.3.1 Properties of the String Constructor

   Description:        The initial value of String.prototype is the built-in
   String prototype object.

   This property shall have the attributes [ DontEnum,
   DontDelete, ReadOnly]

   This tests the DontEnum attribute.

   Author:             christine@netscape.com
   Date:               1 october 1997
*/

var SECTION = "15.5.3.1-1";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "Properties of the String Constructor";

writeHeaderToLog( SECTION + " "+ TITLE);

new TestCase( SECTION, "String.prototype.length",   0,  String.prototype.length );

new TestCase(   SECTION,
		"var str='';for ( p in String ) { if ( p == 'prototype' ) str += p; } str",
		"",
		eval("var str='';for ( p in String ) { if ( p == 'prototype' ) str += p; } str") );

test();
