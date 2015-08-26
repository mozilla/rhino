/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.8.2.15.js';

/**
   File Name:          15.8.2.15.js
   ECMA Section:       15.8.2.15  Math.round(x)
   Description:        return the greatest number value that is closest to the
   argument and is an integer.  if two integers are equally
   close to the argument. then the result is the number value
   that is closer to Infinity.  if the argument is an integer,
   return the argument.
   special cases:
   - if x is NaN       return NaN
   - if x = +0         return +0
   - if x = -0          return -0
   - if x = Infinity   return Infinity
   - if x = -Infinity  return -Infinity
   - if 0 < x < 0.5    return 0
   - if -0.5 <= x < 0  return -0
   example:
   Math.round( 3.5 ) == 4
   Math.round( -3.5 ) == 3
   also:
   - Math.round(x) == Math.floor( x + 0.5 )
   except if x = -0.  in that case, Math.round(x) = -0

   and Math.floor( x+0.5 ) = +0


   Author:             christine@netscape.com
   Date:               7 july 1997
*/

var SECTION = "15.8.2.15";
var VERSION = "ECMA_1";
var TITLE   = "Math.round(x)";
var BUGNUMBER="331411";

var EXCLUDE = "true";

startTest();

writeHeaderToLog( SECTION + " "+ TITLE);

new TestCase( SECTION,
	      "Math.round.length",
	      1,
	      Math.round.length );

new TestCase( SECTION,
	      "Math.round()",
	      Number.NaN,
	      Math.round() );

new TestCase( SECTION,
	      "Math.round(null)",
	      0,
	      Math.round(0) );

new TestCase( SECTION,
	      "Math.round(void 0)",
	      Number.NaN,
	      Math.round(void 0) );

new TestCase( SECTION,
	      "Math.round(true)",
	      1,
	      Math.round(true) );

new TestCase( SECTION,
	      "Math.round(false)",
	      0,
	      Math.round(false) );

new TestCase( SECTION,
	      "Math.round('.99999')",
	      1,
	      Math.round('.99999') );

new TestCase( SECTION,
	      "Math.round('12345e-2')",
	      123,
	      Math.round('12345e-2') );

new TestCase( SECTION,
	      "Math.round(NaN)",
	      Number.NaN,
	      Math.round(Number.NaN) );

new TestCase( SECTION,
	      "Math.round(0)",
	      0,
	      Math.round(0) );

new TestCase( SECTION,
	      "Math.round(-0)",
	      -0,
	      Math.round(-0));

new TestCase( SECTION,
	      "Infinity/Math.round(-0)",
	      -Infinity,
	      Infinity/Math.round(-0) );

new TestCase( SECTION,
	      "Math.round(Infinity)",
	      Number.POSITIVE_INFINITY,
	      Math.round(Number.POSITIVE_INFINITY));

new TestCase( SECTION,
	      "Math.round(-Infinity)",
	      Number.NEGATIVE_INFINITY,
	      Math.round(Number.NEGATIVE_INFINITY));

new TestCase( SECTION,
	      "Math.round(0.49)",
	      0,
	      Math.round(0.49));

new TestCase( SECTION,
	      "Math.round(0.5)",
	      1,
	      Math.round(0.5));

new TestCase( SECTION,
	      "Math.round(0.51)",
	      1,
	      Math.round(0.51));

new TestCase( SECTION,
	      "Math.round(-0.49)",
	      -0,
	      Math.round(-0.49));

new TestCase( SECTION,
	      "Math.round(-0.5)",
	      -0,
	      Math.round(-0.5));

new TestCase( SECTION,
	      "Infinity/Math.round(-0.49)",
	      -Infinity,
	      Infinity/Math.round(-0.49));

new TestCase( SECTION,
	      "Infinity/Math.round(-0.5)",
	      -Infinity,
	      Infinity/Math.round(-0.5));

new TestCase( SECTION,
	      "Math.round(-0.51)",
	      -1,
	      Math.round(-0.51));

new TestCase( SECTION,
	      "Math.round(3.5)",
	      4,
	      Math.round(3.5));

new TestCase( SECTION,
	      "Math.round(-3.5)",
	      -3,
	      Math.round(-3));

test();
