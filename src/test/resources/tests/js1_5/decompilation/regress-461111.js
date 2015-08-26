/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-461111.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 461111;
var summary = 'Decompilation of if (a,b)';
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

  var f = (function() { if(a, b) { } });

  expect = 'function() { if(a, b) { } }';
  actual = f + '';

  compareSource(expect, actual, summary);

  exitFunc ('test');
}
