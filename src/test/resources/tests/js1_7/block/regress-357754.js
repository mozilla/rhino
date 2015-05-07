/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-357754.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 357754;
var summary = 'top level closures with let-bound varibles';
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
 
  expect = 'No Error';
  actual = 'No Error';
  try
  {
    function f() { let k = 3; function g() { print(k); } g() }
    f();
  }
  catch(ex)
  {
    actual = ex + '';
  }
  reportCompare(expect, actual, summary + ': 1');

  expect = 'No Error';
  actual = 'No Error';
  try
  {
    function h() { let k = 3; if (1) function g() { print(k); } g() }
    h();
  }
  catch(ex)
  {
    actual = ex + '';
  }
  reportCompare(expect, actual, summary + ': 2');

  expect = 'No Error';
  actual = 'No Error';
  try
  {
    function i() { let k = 3; (function() { print(k); })() }
    i();
  }
  catch(ex)
  {
    actual = ex + '';
  }
  reportCompare(expect, actual, summary + ': 3');

  exitFunc ('test');
}
