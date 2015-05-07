/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '12.9-1-n.js';

/**
   File Name:          12.9-1-n.js
   ECMA Section:       12.9 The return statement
   Description:

   Author:             christine@netscape.com
   Date:               12 november 1997
*/
var SECTION = "12.9-1-n";
var VERSION = "ECMA_1";
startTest();

writeHeaderToLog( SECTION + " The return statement");

DESCRIPTION = "return";
EXPECTED = "error";

new TestCase(   SECTION,
		"return",
		"error",
		eval("return") );

test();
