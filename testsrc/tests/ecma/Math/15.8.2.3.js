/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.8.2.3.js';

/**
   File Name:          15.8.2.3.js
   ECMA Section:       15.8.2.3 asin( x )
   Description:        return an approximation to the arc sine of the
   argument.  the result is expressed in radians and
   range is from -PI/2 to +PI/2.  special cases:
   - if x is NaN,  the result is NaN
   - if x > 1,     the result is NaN
   - if x < -1,    the result is NaN
   - if x == +0,   the result is +0
   - if x == -0,   the result is -0
   Author:             christine@netscape.com
   Date:               7 july 1997

*/
var SECTION = "15.8.2.3";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "Math.asin()";

writeHeaderToLog( SECTION + " "+ TITLE);

new TestCase( SECTION,
	      "Math.asin()",
	      Number.NaN,
	      Math.asin() );

new TestCase( SECTION,
	      "Math.asin(void 0)",
	      Number.NaN,
	      Math.asin(void 0) );

new TestCase( SECTION,
	      "Math.asin(null)",
	      0,
	      Math.asin(null) );

new TestCase( SECTION,
	      "Math.asin(NaN)",
	      Number.NaN,
	      Math.asin(Number.NaN)   );

new TestCase( SECTION,
	      "Math.asin('string')",
	      Number.NaN,
	      Math.asin("string")     );

new TestCase( SECTION,
	      "Math.asin('0')",
	      0,
	      Math.asin("0") );

new TestCase( SECTION,
	      "Math.asin('1')",
	      Math.PI/2,
	      Math.asin("1") );

new TestCase( SECTION,
	      "Math.asin('-1')",
	      -Math.PI/2,
	      Math.asin("-1") );

new TestCase( SECTION,
	      "Math.asin(Math.SQRT1_2+'')",
	      Math.PI/4,
	      Math.asin(Math.SQRT1_2+'') );

new TestCase( SECTION,
	      "Math.asin(-Math.SQRT1_2+'')",
	      -Math.PI/4,
	      Math.asin(-Math.SQRT1_2+'') );

new TestCase( SECTION,
	      "Math.asin(1.000001)",
	      Number.NaN,
	      Math.asin(1.000001)     );

new TestCase( SECTION,
	      "Math.asin(-1.000001)",
	      Number.NaN,
	      Math.asin(-1.000001)    );

new TestCase( SECTION,
	      "Math.asin(0)",
	      0,
	      Math.asin(0)            );

new TestCase( SECTION,
	      "Math.asin(-0)",
	      -0,
	      Math.asin(-0)           );

new TestCase( SECTION,
	      "Infinity/Math.asin(-0)",
	      -Infinity,
	      Infinity/Math.asin(-0) );

new TestCase( SECTION,
	      "Math.asin(1)",
	      Math.PI/2,
	      Math.asin(1)            );

new TestCase( SECTION,
	      "Math.asin(-1)",
	      -Math.PI/2,
	      Math.asin(-1)            );

new TestCase( SECTION,
	      "Math.asin(Math.SQRT1_2))",
	      Math.PI/4,
	      Math.asin(Math.SQRT1_2) );

new TestCase( SECTION,
	      "Math.asin(-Math.SQRT1_2))",
	      -Math.PI/4,
	      Math.asin(-Math.SQRT1_2));

test();
