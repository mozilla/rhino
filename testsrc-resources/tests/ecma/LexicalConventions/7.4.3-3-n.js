/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '7.4.3-3-n.js';

/**
   File Name:          7.4.3-3-n.js
   ECMA Section:       7.4.3

   Description:
   The following words are used as keywords in proposed extensions and are
   therefore reserved to allow for the possibility of future adoption of
   those extensions.

   FutureReservedWord :: one of
   case    debugger    export      super
   catch   default     extends     switch
   class   do          finally     throw
   const   enum        import      try

   Author:             christine@netscape.com
   Date:               12 november 1997
*/
var SECTION = "7.4.3-3-n";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "Future Reserved Words";

writeHeaderToLog( SECTION + " "+ TITLE);

DESCRIPTION = "var export = true";
EXPECTED = "error";

new TestCase( SECTION,  "var export = true",     "error",    eval("var export = true") );

test();
