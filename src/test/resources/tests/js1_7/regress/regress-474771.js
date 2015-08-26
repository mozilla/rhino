/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-474771.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 474771;
var summary = 'TM: do not halt execution with gczeal, prototype mangling, for..in';
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

  expect = 'PASS';
  jit(true);

  if (typeof gczeal != 'undefined')
  {
    gczeal(2);
  }
  Object.prototype.q = 3;
  for each (let x in [6, 7]) { } print(actual = "PASS");
 
  jit(false);

  delete Object.prototype.q;

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
