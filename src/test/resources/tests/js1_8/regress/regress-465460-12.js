/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-465460-12.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 465460;
var summary = 'TM: valueOf in a loop: do not assert';
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
 
  jit(true);

  for (var j = 0; j < 2; ++j) { if (null > "") { } }

  jit(false);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
