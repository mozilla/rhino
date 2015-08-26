/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '7.3-11.js';

/**
   File Name:          7.3-11.js
   ECMA Section:       7.3 Comments
   Description:


   Author:             christine@netscape.com
   Date:               12 november 1997

*/
var SECTION = "7.3-11";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "Comments";

writeHeaderToLog( SECTION + " "+ TITLE);


var testcase =  new TestCase( SECTION,
			      "code following multiline comment",
			      "pass",
			      "pass");

////testcase.actual="fail";

test();
