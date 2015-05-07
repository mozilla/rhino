/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'try-003.js';

/**
 *  File Name:          try-003.js
 *  ECMA Section:
 *  Description:        The try statement
 *
 *  This test has a try with no catch, and a finally.
 *
 *  Author:             christine@netscape.com
 *  Date:               11 August 1998
 */
var SECTION = "try-003";
var VERSION = "ECMA_2";
var TITLE   = "The try statement";
var BUGNUMBER="http://scopus.mcom.com/bugsplat/show_bug.cgi?id=313585";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

// Tests start here.

TrySomething( "x = \"hi\"", false );
TrySomething( "throw \"boo\"", true );
TrySomething( "throw 3", true );

test();

/**
 *  This function contains a try block with no catch block,
 *  but it does have a finally block.  Try to evaluate expressions
 *  that do and do not throw exceptions.
 */

function TrySomething( expression, throwing ) {
  innerFinally = "FAIL: DID NOT HIT INNER FINALLY BLOCK";
  if (throwing) {
    outerCatch = "FAILED: NO EXCEPTION CAUGHT";
  } else {
    outerCatch = "PASS";
  }
  outerFinally = "FAIL: DID NOT HIT OUTER FINALLY BLOCK";

  try {
    try {
      eval( expression );
    } finally {
      innerFinally = "PASS";
    }
  } catch ( e  ) {
    if (throwing) {
      outerCatch = "PASS";
    } else {
      outerCatch = "FAIL: HIT OUTER CATCH BLOCK";
    }
  } finally {
    outerFinally = "PASS";
  }


  new TestCase(
    SECTION,
    "eval( " + expression +" )",
    "PASS",
    innerFinally );
  new TestCase(
    SECTION,
    "eval( " + expression +" )",
    "PASS",
    outerCatch );
  new TestCase(
    SECTION,
    "eval( " + expression +" )",
    "PASS",
    outerFinally );


}
