/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-349489.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 349489;
var summary = 'Incorrect decompilation of labeled useless statements';
var actual = 'No Crash';
var expect = 'No Crash';


//-----------------------------------------------------------------------------
test();
//-----------------------------------------------------------------------------

function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);

  expect = 'function () {\nL:\n    3;\n}';
  var f = function() { L: 3; };
  actual = f.toString();
  print(f.toString());
  compareSource(expect, actual, summary);

  expect = 'function () {\nL:\n    3;\n    alert(5);\n}';
  f = function() { L: 3; alert(5); }
  actual = f.toString();
  print(f.toString());
  compareSource(expect, actual, summary);

  exitFunc ('test');
}
