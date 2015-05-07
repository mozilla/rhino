/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.3.3.1-2.js';

/**
   File Name:          15.3.3.1-2.js
   ECMA Section:       15.3.3.1 Properties of the Function Constructor
   Function.prototype

   Description:        The initial value of Function.prototype is the built-in
   Function prototype object.

   This property shall have the attributes [DontEnum |
   DontDelete | ReadOnly]

   This test the DontEnum property of Function.prototype.

   Author:             christine@netscape.com
   Date:               28 october 1997

*/
var SECTION = "15.3.3.1-2";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "Function.prototype";

writeHeaderToLog( SECTION + " "+ TITLE);

new TestCase(   SECTION,
		"var str='';for (prop in Function ) str += prop; str;",
		"",
		eval("var str='';for (prop in Function) str += prop; str;")
  );
test();
