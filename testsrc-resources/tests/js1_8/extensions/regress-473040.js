/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-473040.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 473040;
var summary = 'Do not assert: tm->reservedDoublePoolPtr > tm->reservedDoublePool';
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
printStatus (summary);
 
jit(true);

__proto__.functional getter= (new Function("gc()"));
for each (let x in [new Boolean(true), new Boolean(true), -0, new
                    Boolean(true), -0]) { undefined; }

jit(false);

reportCompare(expect, actual, summary);
