/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-313500.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 313500;
var summary = 'Root access to "prototype" property';
var actual = 'No Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
printStatus (summary);
printStatus('This test requires TOO_MUCH_GC');

function F() { }

var prepared = new Object();

F.prototype = {};
F.__defineGetter__('prototype', function() {
		     var tmp = prepared;
		     prepared = null;
		     return tmp;
		   });

new F();
 
reportCompare(expect, actual, summary);
