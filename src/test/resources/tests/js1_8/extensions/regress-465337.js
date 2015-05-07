/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-465337.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 465337;
var summary = 'Do not assert: (m != JSVAL_INT) || isInt32(*vp)';
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

  jit(true); 
  var out = [];
  for (let j = 0; j < 5; ++j) { out.push(6 - ((void 0) ^ 0x80000005)); }
  print(uneval(out));
  jit(false);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
