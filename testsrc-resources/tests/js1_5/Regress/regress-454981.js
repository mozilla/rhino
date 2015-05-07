/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-454981.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 454981;
var summary = 'Do not assert with JIT: size_t(p - cx->fp->slots) < cx->fp->script->nslots';
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

  function f1() { 
    function f0() { return arguments[0]; } 
    for (var i = 0; i < 4; i++) f0('a'); 
  } 
  f1();

  jit(false);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
