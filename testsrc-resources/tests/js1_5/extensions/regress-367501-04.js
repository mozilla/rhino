/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-367501-04.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 367501;
var summary = 'getter/setter crashes';
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

  try
  {
    expect = actual = 'No Crash'; 
    var a = { set x() {} };
    for (var i = 0; i < 0x10050c - 3; ++i) a[i] = 1;
    a.x;
    typeof a.x;
  }
  catch(ex)
  {
  }

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
