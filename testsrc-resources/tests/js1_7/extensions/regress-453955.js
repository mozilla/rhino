/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-453955.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 453955;
var summary = 'Do not assert: sprop->setter != js_watch_set || pobj != obj';
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
 
  for (var z = 0; z < 2; ++z) 
  { 
    [].filter.watch("9", function(y) { yield y; });
  }

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
