/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-342359.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 342359;
var summary = 'Overriding ReferenceError should stick';
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
printStatus (summary);

// work around bug 376957
var SavedReferenceError = ReferenceError;

try
{
  ReferenceError = 5;
}
catch(ex)
{
}

try
{
  foo.blitz;
}
catch(ex)
{
  print(ex + '');
}

if (SavedReferenceError == ReferenceError)
{
  actual = expect = 'Test ignored due to bug 376957';
}
else
{
  expect = 5;
  actual = ReferenceError;
} 
reportCompare(expect, actual, summary);
