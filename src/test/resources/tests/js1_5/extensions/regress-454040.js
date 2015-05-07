/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-454040.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 454040;
var summary = 'Do not crash @ js_ComputeFilename';
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
printStatus (summary);

try
{ 
  this.__defineGetter__("x", Function);
  this.__defineSetter__("x", Function);
  this.watch("x", x.__proto__);
  x = 1;
}
catch(ex)
{
  print(ex + '');
}
reportCompare(expect, actual, summary);
