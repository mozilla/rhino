/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-381205.js';

//-----------------------------------------------------------------------------
var BUGNUMBER = 381205;
var summary = 'uneval with special getter functions';
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
printStatus (summary);
 
expect = '({get x p() {print(4);}})';
getter function p() { print(4) }
actual =  uneval({x getter: this.__lookupGetter__("p")});
reportCompare(expect, actual, summary + ': global');
