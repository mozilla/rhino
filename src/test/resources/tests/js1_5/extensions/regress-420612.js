/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-420612.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 420612;
var summary = 'Do not assert: obj == pobj';
var actual = 'No Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
printStatus (summary);
try
{
  this.__proto__ = []; 
  this.unwatch("x");
}
catch(ex)
{
  print(ex + '');
  if (typeof window != 'undefined')
  {
    expect = 'Error: invalid __proto__ value (can only be set to null)';
  }
  actual = ex + '';
}
reportCompare(expect, actual, summary);
