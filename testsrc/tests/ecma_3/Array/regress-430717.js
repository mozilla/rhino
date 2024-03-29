/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-430717.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 430717;
var summary = 'Dense Arrays should inherit deleted elements from Array.prototype';
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
 
  Array.prototype[2] = "two";
  var a = [0,1,2,3];
  delete a[2];

  expect = 'two';
  actual = a[2];
  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
