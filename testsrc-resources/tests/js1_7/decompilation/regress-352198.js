/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-352198.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 352198;
var summary = 'decompilation of yield (yield)';
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
 
  var f;

  f = function() { yield (yield); }
  expect = 'function() { yield (yield); }';
  actual = f + '';
  compareSource(expect, actual, summary);

  exitFunc ('test');
}
