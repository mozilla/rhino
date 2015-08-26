/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-465234.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 465234;
var summary = '"" <= null';
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

  expect = true;
  actual = true;

  jit(true);

  for (let j = 0; j < 5; ++j) actual = actual && ("" <= null);

  jit(false);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
