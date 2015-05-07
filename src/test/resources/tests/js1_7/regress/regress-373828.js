/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-373828.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 373828;
var summary = 'Do not assert: op == JSOP_LEAVEBLOCKEXPR ? ' + 
  'fp->spbase + OBJ_BLOCK_DEPTH(cx, obj) == sp - 1 : fp->spbase + OBJ_BLOCK_DEPTH(cx, obj) == sp';
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
 
  try
  {
    for(let y in [5,6]) let([] = [1]) (function(){ }); d;
  }
  catch(ex)
  {
    print(ex + '');
  }
  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
