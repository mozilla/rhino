/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-466128.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 466128;
var summary = 'Do not assert: !ti->stackTypeMap.matches(ti_other->stackTypeMap)';
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
  for (let a = 0; a < 3; ++a) { 
    for each (let b in [1, 2, "three", 4, 5, 6, 7, 8]) {
      }
  }
  jit(false);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
