/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'dowhile-005.js';

/**
 *  File Name:          dowhile-005
 *  ECMA Section:
 *  Description:        do...while statements
 *
 *  Test a labeled do...while.  Break out of the loop with no label
 *  should break out of the loop, but not out of the label.
 *
 *  Currently causes an infinite loop in the monkey.  Uncomment the
 *  print statement below and it works OK.
 *
 *  Author:             christine@netscape.com
 *  Date:               26 August 1998
 */
var SECTION = "dowhile-005";
var VERSION = "ECMA_2";
var TITLE   = "do...while with a labeled continue statement";
var BUGNUMBER = "316293";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

NestedLabel();


test();

function NestedLabel() {
  i = 0;
  result1 = "pass";
  result2 = "fail: did not hit code after inner loop";
  result3 = "pass";

outer: {
    do {
    inner: {
//                    print( i );
	break inner;
	result1 = "fail: did break out of inner label";
      }
      result2 = "pass";
      break outer;
      print(i);
    } while ( i++ < 100 );

  }

  result3 = "fail: did not break out of outer label";

  new TestCase(
    SECTION,
    "number of loop iterations",
    0,
    i );

  new TestCase(
    SECTION,
    "break out of inner loop",
    "pass",
    result1 );

  new TestCase(
    SECTION,
    "break out of outer loop",
    "pass",
    result2 );
}
