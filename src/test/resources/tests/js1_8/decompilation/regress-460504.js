/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-460504.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 460504;
var summary = 'Decompilation of genexp in for-loop condition';
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

  expect = 'function () { for(; x, (1 for each (y in [])); ) { } }';
  f = (function () { for(; x, (1 for each (y in [])); ) { } });
  actual = f + '';

  compareSource(expect, actual, summary);

  exitFunc ('test');
}
