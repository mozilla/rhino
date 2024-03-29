/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-354297.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 354297;
var summary = 'getter/setter can be on index';
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
 
  print('This test requires GC_MARK_DEBUG');

  var o = {}; o.__defineGetter__(1, Math.sin); gc()

						 reportCompare(expect, actual, summary);

  exitFunc ('test');
}
