/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-460886-01.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 460886;
var summary = 'Do not asssert: end >= begin';
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

  for (var j = 0; j < 5; ++j) { "".substring(5); }
 
  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
