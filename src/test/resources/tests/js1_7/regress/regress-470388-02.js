/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-470388-02.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 470388;
var summary = 'TM: Do not assert: !(fp->flags & JSFRAME_POP_BLOCKS)';
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

  for each (let x in [function(){}, {}, {}, function(){}, function(){},
                      function(){}]) { ([<y/> for (y in x)]); }

  jit(false);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
