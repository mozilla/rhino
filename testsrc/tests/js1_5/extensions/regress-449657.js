/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-449657.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 449657;
var summary = 'JS_SealObject on Arrays';
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

  if (typeof seal != 'function')
  {
    expect = actual = 'JS_SealObject not supported, test skipped.';
    reportCompare(expect, actual, summary);
  }
  else
  {
    try
    {
      var a= [1, 2, 3];
      seal(a);
    }
    catch(ex)
    {
      actual = ex + '';
    }
    reportCompare(expect, actual, summary + ': 1');

    expect = 'Error: a.length is read-only';
    actual = '';
    try
    {
      a = [1,2,3];
      a[4] = 2;
      seal(a);
      a.length = 5;
    }
    catch(ex)
    {
      actual = ex + '';
    }
    reportCompare(expect, actual, summary + ': 2');
  }

  exitFunc ('test');
}
