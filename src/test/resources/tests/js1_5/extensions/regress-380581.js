/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-380581.js';

//-----------------------------------------------------------------------------
var BUGNUMBER = 380581;
var summary = 'Incorrect uneval with setter in object literal';
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
 
  expect = '({ set x () {}})';
  actual = uneval({x setter: eval("(function () { })") });
  compareSource(expect, actual, summary);
  
  expect = '(function() { })';
  actual = uneval(eval("(function() { })"));
  compareSource(expect, actual, summary);
    
  expect = '(function() { })';
  actual = uneval(eval("(function() { })"));
  compareSource(expect, actual, summary);

  exitFunc ('test');
}
