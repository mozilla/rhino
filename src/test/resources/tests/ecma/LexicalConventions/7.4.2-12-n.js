/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '7.4.2-12-n.js';

/**
   File Name:          7.4.2-12-n.js
   ECMA Section:       7.4.2

   Description:
   The following tokens are ECMAScript keywords and may not be used as
   identifiers in ECMAScript programs.

   Syntax

   Keyword :: one of
   break          for         new         var
   continue       function    return      void
   delete         if          this        while
   else           in          typeof      with

   This test verifies that the keyword cannot be used as an identifier.
   Functioinal tests of the keyword may be found in the section corresponding
   to the function of the keyword.

   Author:             christine@netscape.com
   Date:               12 november 1997

*/
var SECTION = "7.4.1-12-n";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "Keywords";

writeHeaderToLog( SECTION + " "+ TITLE);

DESCRIPTION = "var while = true";
EXPECTED = "error";

new TestCase( SECTION,  "var while = true",     "error",    eval("var while = true") );

test();
