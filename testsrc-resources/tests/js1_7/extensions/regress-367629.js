/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-367629.js';

//-----------------------------------------------------------------------------
var BUGNUMBER = 367629;
var summary = 'Decompilation of result with native function as getter';
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
 
  var a = {}; 
  a.h getter = encodeURI; 

  expect = '({get h encodeURI() {[native code]}})';
  actual = uneval(a);      

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
