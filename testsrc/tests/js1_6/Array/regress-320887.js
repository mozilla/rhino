/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-320887.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 320887;
var summary = 'var x should not throw a ReferenceError';
var actual = 'No error';
var expect = 'No error';

printBugNumber(BUGNUMBER);
printStatus (summary);

try
{
  (function xxx() { ["var x"].map(eval); })()
    }
catch(ex)
{
  actual = ex + '';
}
 
reportCompare(expect, actual, summary);
