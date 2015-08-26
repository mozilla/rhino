/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-345542.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 345542;
var summary = 'Use let in let-scoped loops';
var actual = 'No Crash';
var expect = 'No Crash';


//-----------------------------------------------------------------------------
test();
//-----------------------------------------------------------------------------

function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);
 
  var f = [];
  for (let i = 0; i < 1; i++) f[i] = let (n = i) function () { return n; };
  for (let i = 0; i < 2; i++) f[i] = let (n = i) function () { return n; };

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
