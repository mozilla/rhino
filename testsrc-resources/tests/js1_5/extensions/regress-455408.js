/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-455408.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 455408;
var summary = 'Do not assert with JIT: "Should not move data from GPR to XMM": false';
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

  jit(true);

  for (var j = 0; j < 5; ++j) { if (({}).__proto__ = 1) { } }

  jit(false);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
