/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-443071-01.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 443071;
var summary = 'Do not assert: top != 0 with for (;;[]=[])';
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
 
  print(function() { for (;;[]=[]) { } });

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
