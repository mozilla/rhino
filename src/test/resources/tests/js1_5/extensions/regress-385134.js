/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-385134.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 385134;
var summary = 'Do not crash with setter, watch, uneval';
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
 
  if (typeof this.__defineSetter__ != 'undefined' && 
      typeof this.watch != 'undefined' &&
      typeof uneval != 'undefined')
  {
    this.__defineSetter__(0, function(){});
    this.watch(0, function(){});
    uneval(this);
  }
  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
