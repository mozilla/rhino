/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-459085.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 459085;
var summary = 'Do not assert with JIT: Should not move data from GPR to XMM';
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
  
  jit(true);

  var m = new Number(3);
  function foo() { for (var i=0; i<20;i++) m.toString(); } 
  foo();

  jit(false);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
