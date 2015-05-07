/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-437288-02.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 437288;
var summary = 'for loop turning into a while loop';
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
 
  function f() { const x = 1; for (x in null); }

  expect = 'function f() { const x = 1; for (x in null) {} }';
  actual = f + '';

  compareSource(expect, actual, summary);

  exitFunc ('test');
}
