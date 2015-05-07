/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'StrictEquality-001.js';

/**
 *  File Name:          StrictEquality-001.js
 *  ECMA Section:       11.9.6.js
 *  Description:
 *
 *  Author:             christine@netscape.com
 *  Date:               4 september 1998
 */
var SECTION = "StrictEquality-001 - 11.9.6";
var VERSION = "ECMA_2";
var TITLE   =  "The strict equality operator ( === )";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);


// 1. If Type(x) is different from Type(y) return false

StrictEquality( true, new Boolean(true), false );
StrictEquality( new Boolean(), false, false );
StrictEquality( "", new String(),    false );
StrictEquality( new String("hi"), "hi", false );

// 2. If Type(x) is not Number go to step 9.

// 3. If x is NaN, return false
StrictEquality( NaN, NaN,   false );
StrictEquality( NaN, 0,     false );

// 4. If y is NaN, return false.
StrictEquality( 0,  NaN,    false );

// 5. if x is the same number value as y, return true

// 6. If x is +0 and y is -0, return true

// 7. If x is -0 and y is +0, return true

// 8. Return false.


// 9.  If Type(x) is String, then return true if x and y are exactly
//  the same sequence of characters ( same length and same characters
//  in corresponding positions.) Otherwise return false.

//  10. If Type(x) is Boolean, return true if x and y are both true or
//  both false. otherwise return false.


//  Return true if x and y refer to the same object.  Otherwise return
//  false.

// Return false.


test();

function StrictEquality( x, y, expect ) {
  result = ( x === y );

  new TestCase(
    SECTION,
    x +" === " + y,
    expect,
    result );
}

