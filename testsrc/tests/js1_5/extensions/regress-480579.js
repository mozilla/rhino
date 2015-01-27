/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-480579.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 480579;
var summary = 'Do not assert: pobj_ == obj2';
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

  expect = '12';

  a = {x: 1};
  b = {__proto__: a};
  c = {__proto__: b};
  for (i = 0; i < 2; i++) {
    print(actual += c.x);
    b.x = 2;
  }

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
