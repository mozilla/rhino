/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-452498-098.js';
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

// ------- Comment #98 From Gary Kwong [:nth10sd]

  try
  {
    for(let [x] = (x) in []) {}
// Assertion failure: !(pnu->pn_dflags & PND_BOUND), at ../jsemit.cpp:1818
  }
  catch(ex)
  {
  }

  uneval(function(){(Number(0) for each (NaN in []) for each (x4 in this))});
// Assertion failure: pos == 0, at ../jsopcode.cpp:2963

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
