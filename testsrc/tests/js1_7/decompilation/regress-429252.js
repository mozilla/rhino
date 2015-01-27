/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-429252.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 429252;
var summary = 'trap should not change decompilation of { let x }';
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
 
  function f() { { let x } }

  expect = 'function f() { { let x; } }';
  actual = f + '';
  compareSource(expect, actual, summary + ': before trap');

  if (typeof trap == 'function')
  {
    trap(f, 0, "");

    actual = f + '';
    compareSource(expect, actual, summary + ': after trap');
  }
  exitFunc ('test');
}
