/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-361552.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 361552;
var summary = 'Crash with setter, watch, Script';
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
printStatus (summary);
 
expect = actual = 'No Crash';

if (typeof Script == 'undefined')
{
  print('Test skipped. Script not defined.');
}
else
{
  this.__defineSetter__('x', gc);
  this.watch('x', new Script(''));
  x = 3;
}
reportCompare(expect, actual, summary);
