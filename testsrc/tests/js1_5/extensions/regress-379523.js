/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-379523.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 379523;
var summary = 'Decompilation of sharp declaration';
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
 
  var f = (function () { return #1=[a]; });
  
  expect = '(function () { return #1=[a]; })';
  actual = f.toSource();

  compareSource(expect, actual, summary + ': 1');

  f = (function () { return #1={a:b}; });

  expect = '(function () { return #1={a:b}; })';
  actual = f.toSource();

  compareSource(expect, actual, summary + ': 1');

  exitFunc ('test');
}
