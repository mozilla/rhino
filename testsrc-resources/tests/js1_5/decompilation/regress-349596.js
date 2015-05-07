/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-349596.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 349596;
var summary = 'decompilation of labeled if(0)...';
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

  var f = function() { L: if (0) return 5 }

  expect = 'function () {\n    L: {\n    }\n}';
  actual = f + '';
  compareSource(expect, actual, summary);

  exitFunc ('test');
}
