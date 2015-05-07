/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-465261.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 465261;
var summary = 'TM: Do not assert: ';
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

  for (let z = 0; z < 2; ++z) { 
    for each (let x in [0, true, (void 0), 0, (void 0)]) { 
        if(x){} 
    } 
  };

  jit(false);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
