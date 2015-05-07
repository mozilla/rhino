/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = '11.4.1-002.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 423300;
var summary = '11.4.1 - The delete Operator - delete f()';
var actual = '';
var expect = '';


//-----------------------------------------------------------------------------
test();
//-----------------------------------------------------------------------------

function f() {}

function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);
 
  expect = true;

  try
  {
    actual = delete f();
  }
  catch(ex)
  {
    actual = ex + '';
  }

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
