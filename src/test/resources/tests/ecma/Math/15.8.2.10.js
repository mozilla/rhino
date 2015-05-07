/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.8.2.10.js';

/**
   File Name:          15.8.2.10.js
   ECMA Section:       15.8.2.10  Math.log(x)
   Description:        return an approximiation to the natural logarithm of
   the argument.
   special cases:
   -   if arg is NaN       result is NaN
   -   if arg is <0        result is NaN
   -   if arg is 0 or -0   result is -Infinity
   -   if arg is 1         result is 0
   -   if arg is Infinity  result is Infinity
   Author:             christine@netscape.com
   Date:               7 july 1997
*/

var SECTION = "15.8.2.10";
var VERSION = "ECMA_1";
var TITLE   = "Math.log(x)";
var BUGNUMBER = "77391";

startTest();

writeHeaderToLog( SECTION + " "+ TITLE);


new TestCase( SECTION,
	      "Math.log.length",
	      1,
	      Math.log.length );


new TestCase( SECTION,
	      "Math.log()",
	      Number.NaN,
	      Math.log() );

new TestCase( SECTION,
	      "Math.log(void 0)",
	      Number.NaN,
	      Math.log(void 0) );

new TestCase( SECTION,
	      "Math.log(null)",
	      Number.NEGATIVE_INFINITY,
	      Math.log(null) );

new TestCase( SECTION,
	      "Math.log(true)",
	      0,
	      Math.log(true) );

new TestCase( SECTION,
	      "Math.log(false)",
	      -Infinity,
	      Math.log(false) );

new TestCase( SECTION,
	      "Math.log('0')",
	      -Infinity,
	      Math.log('0') );

new TestCase( SECTION,
	      "Math.log('1')",
	      0,
	      Math.log('1') );

new TestCase( SECTION,
	      "Math.log('Infinity')",
	      Infinity,
	      Math.log("Infinity") );


new TestCase( SECTION,
	      "Math.log(NaN)",
	      Number.NaN,
	      Math.log(Number.NaN) );

new TestCase( SECTION,
	      "Math.log(-0.0000001)",
	      Number.NaN,
	      Math.log(-0.000001)  );

new TestCase( SECTION,
	      "Math.log(-1)",
	      Number.NaN,
	      Math.log(-1)  );

new TestCase( SECTION,
	      "Math.log(0)",
	      Number.NEGATIVE_INFINITY,
	      Math.log(0) );

new TestCase( SECTION,
	      "Math.log(-0)",
	      Number.NEGATIVE_INFINITY,
	      Math.log(-0));

new TestCase( SECTION,
	      "Math.log(1)",
	      0,
	      Math.log(1) );

new TestCase( SECTION,
	      "Math.log(Infinity)",
	      Number.POSITIVE_INFINITY,
	      Math.log(Number.POSITIVE_INFINITY) );

new TestCase( SECTION,
	      "Math.log(-Infinity)",
	      Number.NaN,
	      Math.log(Number.NEGATIVE_INFINITY) );

test();
