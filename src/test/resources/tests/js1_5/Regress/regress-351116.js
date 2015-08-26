/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-351116.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 351116;
var summary = 'formal parameter and inner function have same name';
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
 
  var f = function (s) { function s() { } }

  if (typeof window != 'undefined')
  {
    window.open('javascript:function (s) { function s() { } }');
  }

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
