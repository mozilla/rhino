/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-469625.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 469625;
var summary = 'TM: Do not crash @ js_String_getelem';
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

  [].__proto__[0] = 'a';
  for (var j = 0; j < 3; ++j) [[, ]] = [];

  jit(false);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
