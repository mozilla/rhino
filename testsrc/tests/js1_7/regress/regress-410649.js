/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-410649.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 410649;
var summary = 'function statement and destructuring parameter name clash';
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
 
  function f(a) {
    function a() { }
    return a;
  }

  function g([a, b]) {
    function a() { }
    return a;
  }

  expect = 'function';
  actual = typeof f(1);
  reportCompare(expect, actual, "type for simple parameter case");

  expect = 'function';
  actual = typeof g([1, 2]);
  reportCompare(expect, actual, "type for destructuring parameter case");

  exitFunc ('test');
}
