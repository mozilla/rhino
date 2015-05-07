/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-355145.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 355145;
var summary = 'JS_GetMethodById() on XML Objects';
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
 
  var obj = <x/>;
  expect = "foo";

  obj.function::__iterator__ = function() { yield expect; };
  for(var val in obj)
    actual = val;

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
