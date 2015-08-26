/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-308806-01.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 308806;
var summary = 'Object.prototype.toLocaleString() should track Object.prototype.toString() ';
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
printStatus (summary);

var o = {toString: function() { return 'foo'; }};

expect = o.toString();
actual = o.toLocaleString();
 
reportCompare(expect, actual, summary);
