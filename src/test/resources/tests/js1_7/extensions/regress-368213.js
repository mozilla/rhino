/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-368213.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 368213;
var summary = 'Do not crash with group assignment and sharp variable defn';
var actual = 'No Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
printStatus (summary);
 
(function() { [] = #1=[] });

reportCompare(expect, actual, summary);
