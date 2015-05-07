/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-355622.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 355622;
var summary = 'Do not assert: overwriting';
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
    (function() { export arguments })();
  }
  catch(ex)
  {
    print(ex + '');
  }
  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
