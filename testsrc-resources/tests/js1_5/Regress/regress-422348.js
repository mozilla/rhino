/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-422348.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 422348;
var summary = 'Proper overflow error reporting';
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
 
  expect = 'InternalError: allocation size overflow';
  try 
  { 
    Array(1 << 30).sort(); 
    actual = 'No Error';
  } 
  catch (ex) 
  { 
    actual = ex + '';
  } 

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
