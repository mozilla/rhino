/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'dowhile-004.js';

/**
 *  File Name:          dowhile-004
 *  ECMA Section:
 *  Description:        do...while statements
 *
 *  Test a labeled do...while.  Break out of the loop with no label
 *  should break out of the loop, but not out of the label.
 *
 *  Author:             christine@netscape.com
 *  Date:               11 August 1998
 */
var SECTION = "dowhile-004";
var VERSION = "ECMA_2";
var TITLE   = "do...while with a labeled continue statement";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

DoWhile( 0, 1 );
DoWhile( 1, 1 );
DoWhile( -1, 1 );
DoWhile( 5, 5 );

test();

function DoWhile( limit, expect ) {
  i = 0;
  result1 = "pass";
  result2 = "failed: broke out of labeled statement unexpectedly";

foo: {
    do {
      i++;
      if ( ! (i < limit) ) {
	break;
	result1 = "fail: evaluated statement after a labeled break";
      }
    } while ( true );

    result2 = "pass";
  }

  new TestCase(
    SECTION,
    "do while ( " + i +" < " + limit +" )",
    expect,
    i );

  new TestCase(
    SECTION,
    "breaking out of a do... while loop",
    "pass",
    result1 );


  new TestCase(
    SECTION,
    "breaking out of a labeled do...while loop",
    "pass",
    result2 );
}
