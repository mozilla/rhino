/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-367888.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 367888;
var summary = 'RegExp /(|)??x/g.exec("y") barfs';
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
 
  expect = null;
  actual = /(|)??x/g.exec("y");

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
