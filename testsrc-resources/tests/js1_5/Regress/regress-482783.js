/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-482783.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 482783;
var summary = 'TM: Do not crash @ js_ConcatStrings';
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

  [].concat();
  for (var x = 0; x < 3; ++x) { (null + [,,]); }

  jit(false);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}

