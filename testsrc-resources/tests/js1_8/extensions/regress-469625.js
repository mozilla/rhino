/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-469625.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 469625;
var summary = 'TM: Array prototype and expression closures';
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
 
  expect = 'TypeError: [] is not a function';

  jit(true);

  Array.prototype.__proto__ = function () 3; 

  try
  {
    [].__proto__();
  }
  catch(ex)
  {
    print(actual = ex + '');
  }
  jit(false);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
