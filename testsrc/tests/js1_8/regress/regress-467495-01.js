/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-467495-01.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 467495;
var summary = 'Do not crash @ js_Interpret';
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

  (function() { x = 0; function x() 4; var x; const y = 1; })();

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
