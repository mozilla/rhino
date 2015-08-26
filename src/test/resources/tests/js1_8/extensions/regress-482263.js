/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-482263.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 482263;
var summary = 'TM: Do not assert: x->oprnd2() == lirbuf->sp || x->oprnd2() == gp_ins';
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
printStatus (summary);

jit(true);

__proto__.x getter= function () { return <y/>.([]) };
for each (let x in []) { for each (let x in ['', '']) { } }

jit(true);

reportCompare(expect, actual, summary);

delete __proto__.x;
