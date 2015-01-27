/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-478047.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 478047;
var summary = 'Assign to property with getter but no setter should throw ' +
  'TypeError';
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

  expect = 'TypeError: setting a property that has only a getter';
  try
  { 
    var o = { get p() { return "a"; } };
    o.p = "b";
  }
  catch(ex)
  {
    actual = ex + '';
  }
  reportCompare(expect, actual, summary);


  actual = '';
  try
  {
    o = { get p() { return "a"; } };
    T = (function () {});
    T.prototype = o;
    y = new T();
    y.p = "b";
  }
  catch(ex)
  {
    actual = ex + '';
  }

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
