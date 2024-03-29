/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-354910.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 354910;
var summary = 'decompilation of (delete r(s)).t = a;';
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
 
  var f = function () { (delete r(s)).t = a; }

  expect = 'function () { (delete r(s)).t = a; }';
  actual = f + '';
  compareSource(expect, actual, summary);

  exitFunc ('test');
}
