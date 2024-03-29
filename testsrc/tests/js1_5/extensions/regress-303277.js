/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-303277.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 303277;
var summary = 'Do not crash with crash with a watchpoint for __proto__ property ';
var actual = 'No Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
printStatus (summary);

var o = {};
o.watch("__proto__", function(){return null;});
o.__proto__ = null;
 
reportCompare(expect, actual, summary);
