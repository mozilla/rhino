/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-464334.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 464334;
var summary = 'Do not assert: (size_t) (fp->regs->sp - fp->slots) <= fp->script->nslots';
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
 
  function g()
  {
    gc();
  }

  var a = [];
  for (var i = 0; i != 20; ++i)
    a.push(i);
  g.apply(this, a);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
