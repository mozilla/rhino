/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-452346.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 452346;
var summary = 'Do not crash: @ Balloc';
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
 
  for (var j=0;j<2;++j) (0.1).toPrecision(30);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
