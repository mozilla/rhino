/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-295052.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 295052;
var summary = 'Do not crash when apply method is called on String.prototype.match';
var actual = 'No Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
printStatus (summary);

"".match.apply();
 
reportCompare(expect, actual, summary);
