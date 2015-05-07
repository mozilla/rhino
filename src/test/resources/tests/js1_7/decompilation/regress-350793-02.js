/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-350793-02.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 350793;
var summary = 'for-in loops must be yieldable';
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

  var gen = function() { for(let y in [5,6,7,8]) yield ({}); };
  for (let it in gen())
    ;
  gc();

  reportCompare(expect, actual, summary);

  expect = 'function() { for(let y in [5,6,7,8]) { yield {}; }}';
  actual = gen + '';
  compareSource(expect, actual, summary);

  exitFunc ('test');
}
