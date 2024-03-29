/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '11.4.4.js';

/**
   File Name:          11.4.4.js
   ECMA Section:       11.4.4 Prefix increment operator
   Description:
   The production UnaryExpression : ++ UnaryExpression is evaluated as
   follows:

   1.  Evaluate UnaryExpression.
   2.  Call GetValue(Result(1)).
   3.  Call ToNumber(Result(2)).
   4.  Add the value 1 to Result(3), using the same rules as for the +
   operator (section 11.6.3).
   5.  Call PutValue(Result(1), Result(4)).
   6.  Return Result(4).

   Author:             christine@netscape.com
   Date:               12 november 1997
*/
var SECTION = "11.4.4";
var VERSION = "ECMA_1";
startTest();

writeHeaderToLog( SECTION + " Prefix increment operator");

//  special case:  var is not defined

new TestCase( SECTION,  "var MYVAR; ++MYVAR",                       NaN,                            eval("var MYVAR; ++MYVAR") );
new TestCase( SECTION,  "var MYVAR= void 0; ++MYVAR",               NaN,                            eval("var MYVAR=void 0; ++MYVAR") );
new TestCase( SECTION,  "var MYVAR=null; ++MYVAR",                  1,                            eval("var MYVAR=null; ++MYVAR") );
new TestCase( SECTION,  "var MYVAR=true; ++MYVAR",                  2,                            eval("var MYVAR=true; ++MYVAR") );
new TestCase( SECTION,  "var MYVAR=false; ++MYVAR",                 1,                            eval("var MYVAR=false; ++MYVAR") );

// special numbers
// verify return value

new TestCase( SECTION,    "var MYVAR=Number.POSITIVE_INFINITY;++MYVAR", Number.POSITIVE_INFINITY,   eval("var MYVAR=Number.POSITIVE_INFINITY;++MYVAR") );
new TestCase( SECTION,    "var MYVAR=Number.NEGATIVE_INFINITY;++MYVAR", Number.NEGATIVE_INFINITY,   eval("var MYVAR=Number.NEGATIVE_INFINITY;++MYVAR") );
new TestCase( SECTION,    "var MYVAR=Number.NaN;++MYVAR",               Number.NaN,                 eval("var MYVAR=Number.NaN;++MYVAR") );

// verify value of variable

new TestCase( SECTION,    "var MYVAR=Number.POSITIVE_INFINITY;++MYVAR;MYVAR", Number.POSITIVE_INFINITY,   eval("var MYVAR=Number.POSITIVE_INFINITY;++MYVAR;MYVAR") );
new TestCase( SECTION,    "var MYVAR=Number.NEGATIVE_INFINITY;++MYVAR;MYVAR", Number.NEGATIVE_INFINITY,   eval("var MYVAR=Number.NEGATIVE_INFINITY;++MYVAR;MYVAR") );
new TestCase( SECTION,    "var MYVAR=Number.NaN;++MYVAR;MYVAR",               Number.NaN,                 eval("var MYVAR=Number.NaN;++MYVAR;MYVAR") );


// number primitives
new TestCase( SECTION,    "var MYVAR=0;++MYVAR",            1,          eval("var MYVAR=0;++MYVAR") );
new TestCase( SECTION,    "var MYVAR=0.2345;++MYVAR",       1.2345,     eval("var MYVAR=0.2345;++MYVAR") );
new TestCase( SECTION,    "var MYVAR=-0.2345;++MYVAR",      0.7655,     eval("var MYVAR=-0.2345;++MYVAR") );

// verify value of variable

new TestCase( SECTION,    "var MYVAR=0;++MYVAR;MYVAR",      1,         eval("var MYVAR=0;++MYVAR;MYVAR") );
new TestCase( SECTION,    "var MYVAR=0.2345;++MYVAR;MYVAR", 1.2345,    eval("var MYVAR=0.2345;++MYVAR;MYVAR") );
new TestCase( SECTION,    "var MYVAR=-0.2345;++MYVAR;MYVAR", 0.7655,   eval("var MYVAR=-0.2345;++MYVAR;MYVAR") );
new TestCase( SECTION,    "var MYVAR=0;++MYVAR;MYVAR",      1,   eval("var MYVAR=0;++MYVAR;MYVAR") );
new TestCase( SECTION,    "var MYVAR=0;++MYVAR;MYVAR",      1,   eval("var MYVAR=0;++MYVAR;MYVAR") );
new TestCase( SECTION,    "var MYVAR=0;++MYVAR;MYVAR",      1,   eval("var MYVAR=0;++MYVAR;MYVAR") );

// boolean values
// verify return value

new TestCase( SECTION,    "var MYVAR=true;++MYVAR",         2,       eval("var MYVAR=true;++MYVAR") );
new TestCase( SECTION,    "var MYVAR=false;++MYVAR",        1,      eval("var MYVAR=false;++MYVAR") );
// verify value of variable

new TestCase( SECTION,    "var MYVAR=true;++MYVAR;MYVAR",   2,   eval("var MYVAR=true;++MYVAR;MYVAR") );
new TestCase( SECTION,    "var MYVAR=false;++MYVAR;MYVAR",  1,   eval("var MYVAR=false;++MYVAR;MYVAR") );

// boolean objects
// verify return value

new TestCase( SECTION,    "var MYVAR=new Boolean(true);++MYVAR",         2,     eval("var MYVAR=true;++MYVAR") );
new TestCase( SECTION,    "var MYVAR=new Boolean(false);++MYVAR",        1,     eval("var MYVAR=false;++MYVAR") );
// verify value of variable

new TestCase( SECTION,    "var MYVAR=new Boolean(true);++MYVAR;MYVAR",   2,     eval("var MYVAR=new Boolean(true);++MYVAR;MYVAR") );
new TestCase( SECTION,    "var MYVAR=new Boolean(false);++MYVAR;MYVAR",  1,     eval("var MYVAR=new Boolean(false);++MYVAR;MYVAR") );

// string primitives
new TestCase( SECTION,    "var MYVAR='string';++MYVAR",         Number.NaN,     eval("var MYVAR='string';++MYVAR") );
new TestCase( SECTION,    "var MYVAR='12345';++MYVAR",          12346,          eval("var MYVAR='12345';++MYVAR") );
new TestCase( SECTION,    "var MYVAR='-12345';++MYVAR",         -12344,         eval("var MYVAR='-12345';++MYVAR") );
new TestCase( SECTION,    "var MYVAR='0Xf';++MYVAR",            16,             eval("var MYVAR='0Xf';++MYVAR") );
new TestCase( SECTION,    "var MYVAR='077';++MYVAR",            78,             eval("var MYVAR='077';++MYVAR") );
new TestCase( SECTION,    "var MYVAR=''; ++MYVAR",              1,              eval("var MYVAR='';++MYVAR") );

// verify value of variable

new TestCase( SECTION,    "var MYVAR='string';++MYVAR;MYVAR",   Number.NaN,     eval("var MYVAR='string';++MYVAR;MYVAR") );
new TestCase( SECTION,    "var MYVAR='12345';++MYVAR;MYVAR",    12346,          eval("var MYVAR='12345';++MYVAR;MYVAR") );
new TestCase( SECTION,    "var MYVAR='-12345';++MYVAR;MYVAR",   -12344,          eval("var MYVAR='-12345';++MYVAR;MYVAR") );
new TestCase( SECTION,    "var MYVAR='0xf';++MYVAR;MYVAR",      16,             eval("var MYVAR='0xf';++MYVAR;MYVAR") );
new TestCase( SECTION,    "var MYVAR='077';++MYVAR;MYVAR",      78,             eval("var MYVAR='077';++MYVAR;MYVAR") );
new TestCase( SECTION,    "var MYVAR='';++MYVAR;MYVAR",         1,              eval("var MYVAR='';++MYVAR;MYVAR") );

// string objects
new TestCase( SECTION,    "var MYVAR=new String('string');++MYVAR",         Number.NaN,     eval("var MYVAR=new String('string');++MYVAR") );
new TestCase( SECTION,    "var MYVAR=new String('12345');++MYVAR",          12346,          eval("var MYVAR=new String('12345');++MYVAR") );
new TestCase( SECTION,    "var MYVAR=new String('-12345');++MYVAR",         -12344,         eval("var MYVAR=new String('-12345');++MYVAR") );
new TestCase( SECTION,    "var MYVAR=new String('0Xf');++MYVAR",            16,             eval("var MYVAR=new String('0Xf');++MYVAR") );
new TestCase( SECTION,    "var MYVAR=new String('077');++MYVAR",            78,             eval("var MYVAR=new String('077');++MYVAR") );
new TestCase( SECTION,    "var MYVAR=new String(''); ++MYVAR",              1,              eval("var MYVAR=new String('');++MYVAR") );

// verify value of variable

new TestCase( SECTION,    "var MYVAR=new String('string');++MYVAR;MYVAR",   Number.NaN,     eval("var MYVAR=new String('string');++MYVAR;MYVAR") );
new TestCase( SECTION,    "var MYVAR=new String('12345');++MYVAR;MYVAR",    12346,          eval("var MYVAR=new String('12345');++MYVAR;MYVAR") );
new TestCase( SECTION,    "var MYVAR=new String('-12345');++MYVAR;MYVAR",   -12344,          eval("var MYVAR=new String('-12345');++MYVAR;MYVAR") );
new TestCase( SECTION,    "var MYVAR=new String('0xf');++MYVAR;MYVAR",      16,             eval("var MYVAR=new String('0xf');++MYVAR;MYVAR") );
new TestCase( SECTION,    "var MYVAR=new String('077');++MYVAR;MYVAR",      78,             eval("var MYVAR=new String('077');++MYVAR;MYVAR") );
new TestCase( SECTION,    "var MYVAR=new String('');++MYVAR;MYVAR",         1,              eval("var MYVAR=new String('');++MYVAR;MYVAR") );

test();

