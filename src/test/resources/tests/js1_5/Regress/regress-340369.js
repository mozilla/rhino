/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-340369.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 340369;
var summary = 'Oh for crying out loud.';
var actual = 'No Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
printStatus (summary);

try
{ 
  eval('return /;');
}
catch(ex)
{
  print(ex+'');
}

reportCompare(expect, actual, summary);
