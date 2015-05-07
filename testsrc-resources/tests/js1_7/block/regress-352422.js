/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-352422.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 352422;
var summary = 'let declaration must be direct child of block, ' + 
  'top-level implicit block, or switch body block';
var actual = '';
var expect = 'SyntaxError';

// See https://bugzilla.mozilla.org/show_bug.cgi?id=408957

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
    eval('(function() { with({}) let x; })');
  }
  catch(ex)
  {
    actual = ex.name;
  }
 
  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
