/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-452329.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 452329;
var summary = 'Do not assert: *data->pc == JSOP_CALL || *data->pc == JSOP_NEW';
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

  this.__defineGetter__("x", "".match); if (x) 3;

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
