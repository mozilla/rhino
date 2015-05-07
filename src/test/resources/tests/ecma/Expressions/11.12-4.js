/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '11.12-4.js';

/**
   File Name:          11.12-4.js
   ECMA Section:       11.12
   Description:

   The grammar for a ConditionalExpression in ECMAScript is a little bit
   different from that in C and Java, which each allow the second
   subexpression to be an Expression but restrict the third expression to
   be a ConditionalExpression.  The motivation for this difference in
   ECMAScript is to allow an assignment expression to be governed by either
   arm of a conditional and to eliminate the confusing and fairly useless
   case of a comma expression as the center expression.

   Author:             christine@netscape.com
   Date:               12 november 1997
*/

var SECTION = "11.12-4";
var VERSION = "ECMA_1";
startTest();
writeHeaderToLog( SECTION + " Conditional operator ( ? : )");

// the following expression should NOT be an error in JS.

new TestCase( SECTION,
	      "true ? MYVAR1 = 'PASSED' : MYVAR1 = 'FAILED'; MYVAR1",
	      "PASSED",
	      eval("true ? MYVAR1 = 'PASSED' : MYVAR1 = 'FAILED'; MYVAR1") );

test();

