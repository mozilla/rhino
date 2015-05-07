/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-420610.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 420610;
var summary = 'Do not crash with eval("this.x")';
var actual = 'No Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
printStatus (summary);
 
(function(){ eval("this.x") })();

reportCompare(expect, actual, summary);
