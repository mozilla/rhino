/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-453049.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 453049;
var summary = 'Do not assert with JIT: (*m != JSVAL_INT) || isInt32(*vp)';
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
 
  jit(true);

  var z = 0; for (let j = 0; j < 5; ++j) { ({p: (-z)}); }

  jit(false);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
