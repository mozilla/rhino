/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-472450-04.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 472450;
var summary = 'TM: Do not assert: StackBase(fp) + blockDepth == regs.sp';
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

  ({__proto__: #1=[#1#]});
  function f(){ 
    eval("for (var y = 0; y < 1; ++y) { for each (let z in [null, function(){}, null, '', null, '', null]) { let x = 1, c = []; } }"); 
  }
  f();

  jit(false);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
