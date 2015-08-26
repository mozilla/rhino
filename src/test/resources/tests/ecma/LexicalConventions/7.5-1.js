/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '7.5-1.js';

/**
   File Name:          7.5-1.js
   ECMA Section:       7.5 Identifiers
   Description:        Identifiers are of unlimited length
   - can contain letters, a decimal digit, _, or $
   - the first character cannot be a decimal digit
   - identifiers are case sensitive

   Author:             christine@netscape.com
   Date:               11 september 1997
*/
var SECTION = "7.5-1";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "Identifiers";

writeHeaderToLog( SECTION + " "+ TITLE);

new TestCase( SECTION,    "var $123 = 5",      5,       eval("var $123 = 5;$123") );
new TestCase( SECTION,    "var _123 = 5",      5,       eval("var _123 = 5;_123") );

test();
