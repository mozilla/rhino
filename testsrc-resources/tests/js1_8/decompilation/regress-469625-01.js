/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-469625-01.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 469625;
var summary = 'decompile [a, b, [c0, c1]] = [x, x, x];';
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
 
  function f(x) {
    var [a, b, [c0, c1]] = [x, x, x];
  }

  expect = 'function f(x) { var [a, b, [c0, c1]] = [x, x, x]; }';
  actual = f + '';

  compareSource(expect, actual, summary);

  exitFunc ('test');
}
