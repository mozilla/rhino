/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-326453.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 326453;
var summary = 'Do not assert: while decompiling';
var actual = 'No Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
printStatus (summary);

function f() { with({})function g() { }; printStatus(); }

printStatus(f.toString());

reportCompare(expect, actual, summary);
