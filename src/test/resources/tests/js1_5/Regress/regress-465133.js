/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-465133.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 465133;
var summary = '{} < {}';
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
 
  expect = 'false,false,false,false,false,';
  actual = '';

  jit(true);

  for (var i=0;i<5;++i) actual += ({} < {}) + ',';

  jit(false);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
