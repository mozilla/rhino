/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.7.3.1-2.js';

/**
   File Name:          15.7.3.1-2.js
   ECMA Section:       15.7.3.1 Number.prototype
   Description:        All value properties of the Number object should have
   the attributes [DontEnum, DontDelete, ReadOnly]

   this test checks the ReadOnly attribute of Number.prototype

   Author:             christine@netscape.com
   Date:               16 september 1997
*/


var SECTION = "15.7.3.1-2";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "Number.prototype";

writeHeaderToLog( SECTION + " "+ TITLE);

new TestCase(   SECTION,
		"var NUM_PROT = Number.prototype; Number.prototype = null; Number.prototype == NUM_PROT",
		true,
		eval("var NUM_PROT = Number.prototype; Number.prototype = null; Number.prototype == NUM_PROT") );

new TestCase(   SECTION,
		"Number.prototype=0; Number.prototype",
		Number.prototype,
		eval("Number.prototype=0; Number.prototype") );

test();
