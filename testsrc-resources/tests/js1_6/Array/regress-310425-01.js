/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-310425-01.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 310425;
var summary = 'Array.indexOf/lastIndexOf edge cases';
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
printStatus (summary);
 
expect = -1;
actual = [].lastIndexOf(undefined, -1);
reportCompare(expect, actual, summary);

expect = -1;
actual = [].indexOf(undefined, -1);
reportCompare(expect, actual, summary);

var x = [];
x[1 << 30] = 1;
expect = (1 << 30);
actual = x.lastIndexOf(1);
reportCompare(expect, actual, summary);
