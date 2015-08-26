/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-350837.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 350837;
var summary = 'clear cx->throwing in finally';
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
 
  expect = 'F';

  function f()
  {
    actual = "F";
  }

  try
  {
    try {
      throw 1;
    } finally {
      f.call(this);
    }
  }
  catch(ex)
  {
    reportCompare(1, ex, summary);
  }

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
