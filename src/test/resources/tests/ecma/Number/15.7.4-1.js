/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.7.4-1.js';

/**
   File Name:          15.7.4-1.js
   ECMA Section:       15.7.4.1 Properties of the Number Prototype Object
   Description:
   Author:             christine@netscape.com
   Date:               16 september 1997
*/


var SECTION = "15.7.4-1";
var VERSION = "ECMA_1";
startTest();
writeHeaderToLog( SECTION + "Properties of the Number prototype object");

new TestCase(SECTION, "Number.prototype.valueOf()",      0,                  Number.prototype.valueOf() );
new TestCase(SECTION, "typeof(Number.prototype)",        "object",           typeof(Number.prototype) );
new TestCase(SECTION, "Number.prototype.constructor == Number",    true,     Number.prototype.constructor == Number );
//    new TestCase(SECTION, "Number.prototype == Number.__proto__",      true,   Number.prototype == Number.__proto__ );

test();
