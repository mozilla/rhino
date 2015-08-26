/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-295666.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 295666;
var summary = 'Check JS only recursion stack overflow';
var actual = 'No Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
printStatus (summary);

try
{
  throw {toString: parseInt.call};
}
catch(e)
{
} 
reportCompare(expect, actual, summary);
