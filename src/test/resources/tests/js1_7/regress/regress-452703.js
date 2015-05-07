/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-452703.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 452703;
var summary = 'Do not assert with JIT: rmask(rr)&FpRegs';
var actual = 'No Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
printStatus (summary);

jit(true);

(function() { for(let y in [0,1,2,3,4]) y = NaN; })();

jit(false);

reportCompare(expect, actual, summary);
