/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-351795.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 351795;
var summary = 'Do not assert: top < ss->printer->script->depth';
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
    p={}; 
    (p.z = [1].some(function(y) { return y > 0; }) ? 4 : [6])(5);
  }
  catch(ex)
  {
    print(ex + '');
  }

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
