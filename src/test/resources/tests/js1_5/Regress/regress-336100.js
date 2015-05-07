/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-336100.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 336100;
var summary = 'bug 336100 - arguments regressed';
var actual = '';
var expect;

printBugNumber(BUGNUMBER);
printStatus (summary);

expect = '[object Object]';
actual = (function(){return (arguments + '');})(); 
reportCompare(expect, actual, summary);

// see bug 336100 comment 29
expect = typeof window == 'undefined' ? '' : '[object Object]';
actual = (function(){with (this) return(arguments + '');})();
reportCompare(expect, actual, summary);
