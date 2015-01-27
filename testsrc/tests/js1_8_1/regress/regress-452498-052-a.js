/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-452498-052-a.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 452498;
var summary = 'TM: upvar2 regression tests';
var actual = '';
var expect = '';


//-----------------------------------------------------------------------------
test();
//-----------------------------------------------------------------------------

function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);

// ------- Comment #52 From Jason Orendorff


// Assertion failure: pn_arity == PN_FUNC || pn_arity == PN_NAME, at ../jsparse.h:444
// Here the function node has been morphed into a JSOP_TRUE node, but we're in
// FindFunArgs poking it anyway.
  if (typeof timeout == 'function')
  {
    expectExitCode(6);
    timeout(3);
    while(function(){});
  }

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
