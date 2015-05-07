/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-476871-01.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 476871;
var summary = 'Do not assert: *(JSObject**)slot == NULL';
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
printStatus (summary);

jit(true);

let ([] = false) { (this.watch("x", /a/g)); };
(function () { (eval("(function(){for each (x in [1, 2, 2]);});"))(); })();

jit(false);

reportCompare(expect, actual, summary);
