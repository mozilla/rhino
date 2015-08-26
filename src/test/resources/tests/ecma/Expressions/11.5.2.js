/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '11.5.2.js';

/**
   File Name:          11.5.2.js
   ECMA Section:       11.5.2 Applying the / operator
   Description:

   The / operator performs division, producing the quotient of its operands.
   The left operand is the dividend and the right operand is the divisor.
   ECMAScript does not perform integer division. The operands and result of all
   division operations are double-precision floating-point numbers.
   The result of division is determined by the specification of IEEE 754 arithmetic:

   If either operand is NaN, the result is NaN.
   The sign of the result is positive if both operands have the same sign, negative if the operands have different
   signs.
   Division of an infinity by an infinity results in NaN.
   Division of an infinity by a zero results in an infinity. The sign is determined by the rule already stated above.
   Division of an infinity by a non-zero finite value results in a signed infinity. The sign is determined by the rule
   already stated above.
   Division of a finite value by an infinity results in zero. The sign is determined by the rule already stated above.
   Division of a zero by a zero results in NaN; division of zero by any other finite value results in zero, with the sign
   determined by the rule already stated above.
   Division of a non-zero finite value by a zero results in a signed infinity. The sign is determined by the rule
   already stated above.
   In the remaining cases, where neither an infinity, nor a zero, nor NaN is involved, the quotient is computed and
   rounded to the nearest representable value using IEEE 754 round-to-nearest mode. If the magnitude is too
   large to represent, we say the operation overflows; the result is then an infinity of appropriate sign. If the
   magnitude is too small to represent, we say the operation underflows and the result is a zero of the appropriate
   sign. The ECMAScript language requires support of gradual underflow as defined by IEEE 754.

   Author:             christine@netscape.com
   Date:               12 november 1997
*/
var SECTION = "11.5.2";
var VERSION = "ECMA_1";
var BUGNUMBER="111202";
startTest();

writeHeaderToLog( SECTION + " Applying the / operator");

// if either operand is NaN, the result is NaN.

new TestCase( SECTION,    "Number.NaN / Number.NaN",    Number.NaN,     Number.NaN / Number.NaN );
new TestCase( SECTION,    "Number.NaN / 1",             Number.NaN,     Number.NaN / 1 );
new TestCase( SECTION,    "1 / Number.NaN",             Number.NaN,     1 / Number.NaN );

new TestCase( SECTION,    "Number.POSITIVE_INFINITY / Number.NaN",    Number.NaN,     Number.POSITIVE_INFINITY / Number.NaN );
new TestCase( SECTION,    "Number.NEGATIVE_INFINITY / Number.NaN",    Number.NaN,     Number.NEGATIVE_INFINITY / Number.NaN );

// Division of an infinity by an infinity results in NaN.

new TestCase( SECTION,    "Number.NEGATIVE_INFINITY / Number.NEGATIVE_INFINITY",    Number.NaN,   Number.NEGATIVE_INFINITY / Number.NEGATIVE_INFINITY );
new TestCase( SECTION,    "Number.POSITIVE_INFINITY / Number.NEGATIVE_INFINITY",    Number.NaN,   Number.POSITIVE_INFINITY / Number.NEGATIVE_INFINITY );
new TestCase( SECTION,    "Number.NEGATIVE_INFINITY / Number.POSITIVE_INFINITY",    Number.NaN,   Number.NEGATIVE_INFINITY / Number.POSITIVE_INFINITY );
new TestCase( SECTION,    "Number.POSITIVE_INFINITY / Number.POSITIVE_INFINITY",    Number.NaN,   Number.POSITIVE_INFINITY / Number.POSITIVE_INFINITY );

// Division of an infinity by a zero results in an infinity.

new TestCase( SECTION,    "Number.POSITIVE_INFINITY / 0",   Number.POSITIVE_INFINITY, Number.POSITIVE_INFINITY / 0 );
new TestCase( SECTION,    "Number.NEGATIVE_INFINITY / 0",   Number.NEGATIVE_INFINITY, Number.NEGATIVE_INFINITY / 0 );
new TestCase( SECTION,    "Number.POSITIVE_INFINITY / -0",  Number.NEGATIVE_INFINITY,   Number.POSITIVE_INFINITY / -0 );
new TestCase( SECTION,    "Number.NEGATIVE_INFINITY / -0",  Number.POSITIVE_INFINITY,   Number.NEGATIVE_INFINITY / -0 );

// Division of an infinity by a non-zero finite value results in a signed infinity.

new TestCase( SECTION,    "Number.NEGATIVE_INFINITY / 1 ",          Number.NEGATIVE_INFINITY,   Number.NEGATIVE_INFINITY / 1 );
new TestCase( SECTION,    "Number.NEGATIVE_INFINITY / -1 ",         Number.POSITIVE_INFINITY,   Number.NEGATIVE_INFINITY / -1 );
new TestCase( SECTION,    "Number.POSITIVE_INFINITY / 1 ",          Number.POSITIVE_INFINITY,   Number.POSITIVE_INFINITY / 1 );
new TestCase( SECTION,    "Number.POSITIVE_INFINITY / -1 ",         Number.NEGATIVE_INFINITY,   Number.POSITIVE_INFINITY / -1 );

new TestCase( SECTION,    "Number.NEGATIVE_INFINITY / Number.MAX_VALUE ",          Number.NEGATIVE_INFINITY,   Number.NEGATIVE_INFINITY / Number.MAX_VALUE );
new TestCase( SECTION,    "Number.NEGATIVE_INFINITY / -Number.MAX_VALUE ",         Number.POSITIVE_INFINITY,   Number.NEGATIVE_INFINITY / -Number.MAX_VALUE );
new TestCase( SECTION,    "Number.POSITIVE_INFINITY / Number.MAX_VALUE ",          Number.POSITIVE_INFINITY,   Number.POSITIVE_INFINITY / Number.MAX_VALUE );
new TestCase( SECTION,    "Number.POSITIVE_INFINITY / -Number.MAX_VALUE ",         Number.NEGATIVE_INFINITY,   Number.POSITIVE_INFINITY / -Number.MAX_VALUE );

// Division of a finite value by an infinity results in zero.

new TestCase( SECTION,    "1 / Number.NEGATIVE_INFINITY",   -0,             1 / Number.NEGATIVE_INFINITY );
new TestCase( SECTION,    "1 / Number.POSITIVE_INFINITY",   0,              1 / Number.POSITIVE_INFINITY );
new TestCase( SECTION,    "-1 / Number.POSITIVE_INFINITY",  -0,             -1 / Number.POSITIVE_INFINITY );
new TestCase( SECTION,    "-1 / Number.NEGATIVE_INFINITY",  0,              -1 / Number.NEGATIVE_INFINITY );

new TestCase( SECTION,    "Number.MAX_VALUE / Number.NEGATIVE_INFINITY",   -0,             Number.MAX_VALUE / Number.NEGATIVE_INFINITY );
new TestCase( SECTION,    "Number.MAX_VALUE / Number.POSITIVE_INFINITY",   0,              Number.MAX_VALUE / Number.POSITIVE_INFINITY );
new TestCase( SECTION,    "-Number.MAX_VALUE / Number.POSITIVE_INFINITY",  -0,             -Number.MAX_VALUE / Number.POSITIVE_INFINITY );
new TestCase( SECTION,    "-Number.MAX_VALUE / Number.NEGATIVE_INFINITY",  0,              -Number.MAX_VALUE / Number.NEGATIVE_INFINITY );

// Division of a zero by a zero results in NaN

new TestCase( SECTION,    "0 / -0",                         Number.NaN,     0 / -0 );
new TestCase( SECTION,    "-0 / 0",                         Number.NaN,     -0 / 0 );
new TestCase( SECTION,    "-0 / -0",                        Number.NaN,     -0 / -0 );
new TestCase( SECTION,    "0 / 0",                          Number.NaN,     0 / 0 );

// division of zero by any other finite value results in zero

new TestCase( SECTION,    "0 / 1",                          0,              0 / 1 );
new TestCase( SECTION,    "0 / -1",                        -0,              0 / -1 );
new TestCase( SECTION,    "-0 / 1",                        -0,              -0 / 1 );
new TestCase( SECTION,    "-0 / -1",                       0,               -0 / -1 );

// Division of a non-zero finite value by a zero results in a signed infinity.

new TestCase( SECTION,    "1 / 0",                          Number.POSITIVE_INFINITY,   1/0 );
new TestCase( SECTION,    "1 / -0",                         Number.NEGATIVE_INFINITY,   1/-0 );
new TestCase( SECTION,    "-1 / 0",                         Number.NEGATIVE_INFINITY,   -1/0 );
new TestCase( SECTION,    "-1 / -0",                        Number.POSITIVE_INFINITY,   -1/-0 );

new TestCase( SECTION,    "0 / Number.POSITIVE_INFINITY",   0,      0 / Number.POSITIVE_INFINITY );
new TestCase( SECTION,    "0 / Number.NEGATIVE_INFINITY",   -0,     0 / Number.NEGATIVE_INFINITY );
new TestCase( SECTION,    "-0 / Number.POSITIVE_INFINITY",  -0,     -0 / Number.POSITIVE_INFINITY );
new TestCase( SECTION,    "-0 / Number.NEGATIVE_INFINITY",  0,      -0 / Number.NEGATIVE_INFINITY );

test();

