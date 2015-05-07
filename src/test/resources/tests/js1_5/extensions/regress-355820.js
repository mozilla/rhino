/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-355820.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 355820;
var summary = 'Remove non-standard Script object';
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

  print('This test will fail in gecko prior to 1.9');
 
  expect = 'undefined';
  actual = typeof Script;

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
