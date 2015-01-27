/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-348904.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 348904;
var summary = 'decompilation for "let" in lvalue part of for..in';
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

  var f = function () { for (let i = 3 in {}); }
  actual = f + '';
  expect = 'function () {\n    for (let i in {}) {\n    }\n}';
  compareSource(expect, actual, summary);

  var f = function () { for (let i = (y = 4) in {}); }
  actual = f + '';
  expect = 'function () {\n    y = 4;\n    for (let i in {}) {\n    }\n}';
  compareSource(expect, actual, summary);

  exitFunc ('test');
}
