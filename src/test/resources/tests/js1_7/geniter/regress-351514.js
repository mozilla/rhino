/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-351514.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 351514;
var summary = 'Finalize yield syntax to match ES4/JS2 proposal';
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
 
  expect = /SyntaxError: yield expression must be parenthesized/;
  try
  {
    eval('function f() { yield g(yield 1, 2) };');
  }
  catch(ex)
  {
    actual = ex + '';
  }

  reportMatch(expect, actual, summary);

  exitFunc ('test');
}
