/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-352616.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 352616;
var summary = 'Do not Crash reporting error with |for..in| and |let|';
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

  expect = /TypeError: (\(let \(b = 1\) 2\).c is not a function|Cannot find function c.)/;
  actual = 'No Error';
  try
  {
    for(a in (let (b=1) 2).c(3)) { };
  }
  catch(ex)
  {
    actual = ex + '';
  }

  reportMatch(expect, actual, summary + ': 1');

  expect = /TypeError: (\(let \(b = 1, d = 2\) 2\).c is not a function|Cannot find function c.)/;
  actual = 'No Error';
  try
  {
    for(a in (let (b=1,d=2) 2).c(3)) { };
  }
  catch(ex)
  {
    actual = ex + '';
  }

  reportMatch(expect, actual, summary + ': 2');

  expect = /TypeError: (\(let \(b = 1, d = 2\) 2\).c is not a function|Cannot find function c.)/;
  actual = 'No Error';
  try
  {
    for(a in (let (b=1,d=2) 2).c(3)) { };
  }
  catch(ex)
  {
    actual = ex + '';
  }

  reportMatch(expect, actual, summary + ': 3');

  exitFunc ('test');
}
