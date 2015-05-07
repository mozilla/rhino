/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-477234.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 477234;
var summary = 'Do not assert: v != JSVAL_ERROR_COOKIE';
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
 
  for (iters = 0; iters < 11500; ++iters) {
    for each (let x in ['', '', '']){}
    eval("__proto__.x getter = function(){}");
    var a = uneval;
    delete uneval;
    uneval = a;
    var b = toSource;
    delete toSource;
    toSource = b;
    var c = toString;
    delete toString;
    toString = c;
  }

  jit(true);

  delete __proto__.x;

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
