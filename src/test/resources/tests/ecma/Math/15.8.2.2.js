/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.8.2.2.js';

/**
   File Name:          15.8.2.2.js
   ECMA Section:       15.8.2.2 acos( x )
   Description:        return an approximation to the arc cosine of the
   argument.  the result is expressed in radians and
   range is from +0 to +PI.  special cases:
   - if x is NaN, return NaN
   - if x > 1, the result is NaN
   - if x < -1, the result is NaN
   - if x == 1, the result is +0
   Author:             christine@netscape.com
   Date:               7 july 1997
*/
var SECTION = "15.8.2.2";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "Math.acos()";

writeHeaderToLog( SECTION + " "+ TITLE);

new TestCase( SECTION,
	      "Math.acos.length",
	      1,
	      Math.acos.length );

new TestCase( SECTION,
	      "Math.acos(void 0)",
	      Number.NaN,
	      Math.acos(void 0) );

new TestCase( SECTION,
	      "Math.acos()",
	      Number.NaN,
	      Math.acos() );

new TestCase( SECTION,
	      "Math.acos(null)",
	      Math.PI/2,
	      Math.acos(null) );

new TestCase( SECTION,
	      "Math.acos(NaN)",
	      Number.NaN,
	      Math.acos(Number.NaN) );

new TestCase( SECTION,
	      "Math.acos(a string)",
	      Number.NaN,
	      Math.acos("a string") );

new TestCase( SECTION,
	      "Math.acos('0')",
	      Math.PI/2,
	      Math.acos('0') );

new TestCase( SECTION,
	      "Math.acos('1')",
	      0,
	      Math.acos('1') );

new TestCase( SECTION,
	      "Math.acos('-1')",
	      Math.PI,
	      Math.acos('-1') );

new TestCase( SECTION,
	      "Math.acos(1.00000001)",
	      Number.NaN,
	      Math.acos(1.00000001) );

new TestCase( SECTION,
	      "Math.acos(11.00000001)",
	      Number.NaN,
	      Math.acos(-1.00000001) );

new TestCase( SECTION,
	      "Math.acos(1)",
	      0,
	      Math.acos(1)          );

new TestCase( SECTION,
	      "Math.acos(-1)",
	      Math.PI,
	      Math.acos(-1)         );

new TestCase( SECTION,
	      "Math.acos(0)",
	      Math.PI/2,
	      Math.acos(0)          );

new TestCase( SECTION,
	      "Math.acos(-0)",
	      Math.PI/2,
	      Math.acos(-0)         );

new TestCase( SECTION,
	      "Math.acos(Math.SQRT1_2)",
	      Math.PI/4,
	      Math.acos(Math.SQRT1_2));

new TestCase( SECTION,
	      "Math.acos(-Math.SQRT1_2)",
	      Math.PI/4*3,
	      Math.acos(-Math.SQRT1_2));

new TestCase( SECTION,
	      "Math.acos(0.9999619230642)",
	      Math.PI/360,
	      Math.acos(0.9999619230642));

new TestCase( SECTION,
	      "Math.acos(-3.0)",
	      Number.NaN,
	      Math.acos(-3.0));

test();
