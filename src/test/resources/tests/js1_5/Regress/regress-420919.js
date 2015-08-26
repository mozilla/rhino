/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-420919.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 420919;
var summary = 'this.u.v = 1 should report this.u is undefined';
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
 
  // 1.8 branch reports no properties, trunk reports undefined
  expect = /TypeError: this.u is undefined|TypeError: this.u has no properties/;

  try
  {
    this.u.v = 1;
  }
  catch(ex)
  {
    actual = ex + '';
  }
  reportMatch(expect, actual, summary);

  exitFunc ('test');
}
