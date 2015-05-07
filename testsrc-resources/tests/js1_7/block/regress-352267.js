/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-352267.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 352267;
var summary = 'Do not assert with |if|, block, |let|';
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
 
  uneval(function() { if (y) { { let set = 4.; } } else if (<x/>) { } });

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
