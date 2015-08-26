/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-452498-111.js';
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
 
// ------- Comment #111 From Gary Kwong [:nth10sd]

options("anonfunfix");
new Function("{function x(){}}");

// Assertion failure: pn->pn_defn || (fun->flags & JSFUN_LAMBDA), at ../jsemit.cpp:4149

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}


