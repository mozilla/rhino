/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-474771-01.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 474771;
var summary = 'TM: do not assert: jumpTable == interruptJumpTable';
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

  var o = {};
  o.__defineSetter__('x', function(){});
  for (let j = 0; j < 4; ++j) o.x = 3;

  jit(false);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
