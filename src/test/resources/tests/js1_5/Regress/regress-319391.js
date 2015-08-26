/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-319391.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 319391;
var summary = 'Assignment to eval(...) should be runtime error';
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
printStatus (summary);

var b = {};

expect = 'error';
try
{
  if (1) { eval("b.z") = 3; }
  actual = 'no error';
}
catch(ex)
{
  actual = 'error';
}
reportCompare(expect, actual, summary);

expect = 'no error';
try
{
  if (0) { eval("b.z") = 3; }
  actual = 'no error';
}
catch(ex)
{
  actual = 'error';
}
reportCompare(expect, actual, summary);
