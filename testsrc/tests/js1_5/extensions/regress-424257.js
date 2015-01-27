/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-424257.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 424257;
var summary = 'Do not assert: op2 == JSOP_INITELEM';
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
    eval("var x; while(x getter={});");
  }
  catch(ex)
  {
    expect = 'SyntaxError: invalid getter usage';
    actual = ex + '';
  }

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
