/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-459606.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 459606;
var summary = '((0.1).toFixed()).toSource()';
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

  expect = '(new String("0"))';
  actual = ((0.1).toFixed()).toSource();

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
