/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-112626.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 112626;
var summary = 'Do not crash String.split(regexp) when regexp contains parens';
var actual = 'No Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
printStatus (summary);

var _cs='2001-01-01';
var curTime = _cs.split(/([- :])/);
 
reportCompare(expect, actual, summary);
