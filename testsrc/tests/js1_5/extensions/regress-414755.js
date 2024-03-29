/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-414755.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 414755;
var summary = 'GC hazard due to missing SAVE_SP_AND_PC';
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

  function f()
  {
    var a = 1e10;
    var b = 2e10;
    var c = 3e10;

    return (a*2) * ((b*2) * c); 
  }
 
  if (typeof gczeal != 'undefined')
  {
    expect = f();

    gczeal(2);

    actual = f();
  }
  else
  {
    expect = actual = 'Test requires gczeal, skipped.';
  }

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
