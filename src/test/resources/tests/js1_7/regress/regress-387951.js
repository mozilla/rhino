/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-387951.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 387951;
var summary = 'Do not assert: cg->stackDepth >= 0';
var actual = 'No Crash';
var expect = 'No Crash';


//-----------------------------------------------------------------------------
test();
//-----------------------------------------------------------------------------

function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);
 
  with(delete let (functional) null ? 1 : {}){}

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
