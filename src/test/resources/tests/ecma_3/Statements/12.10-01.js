/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = '12.10-01.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 462734;
var summary = 'evaluating lhs "Reference" *before* evaluating rhs';
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

  var x = 1;
  var o = {};
  with (o)
    x = o.x = 2;
  print(x);

  expect = 4;
  actual = x + o.x;

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
