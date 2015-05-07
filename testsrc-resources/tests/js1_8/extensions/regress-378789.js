/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-378789.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 378789;
var summary = 'js_PutEscapedString should handle nulls';
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
 
  if (typeof dumpHeap == 'undefined')
  {
    print('dumpHeap not supported');
  }
  else
  {
    dumpHeap(null, [ "a\0b" ], null, 1);
  }
  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
