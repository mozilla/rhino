/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-358594-06.js';

//-----------------------------------------------------------------------------
var BUGNUMBER = 358594;
var summary = 'Do not crash on uneval(this).';
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
printStatus (summary);

// don't crash|assert
f = function () { };
f.hhhhhhhhh = this; 
this.m setter = f; 
uneval(this);
reportCompare(expect, actual, summary);
