/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-346645-02.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 346645;
var summary = 'Do not crash with empty array in destructuring assign LHS';
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
    eval('({ a:[] }) = 3;');
  }
  catch(ex)
  {
    print(ex);
  }

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
