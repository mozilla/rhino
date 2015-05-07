/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.4.3.js';

/**
   File Name:          15.4.3.js
   ECMA Section:       15.4.3 Properties of the Array Constructor
   Description:        The value of the internal [[Prototype]] property of the
   Array constructor is the Function prototype object.

   Author:             christine@netscape.com
   Date:               7 october 1997
*/

var SECTION = "15.4.3";
var VERSION = "ECMA_2";
startTest();
var TITLE   = "Properties of the Array Constructor";

writeHeaderToLog( SECTION + " "+ TITLE);

new TestCase( SECTION,
	      "Array.__proto__",     
	      Function.prototype,       
	      Array.__proto__ );

test();
