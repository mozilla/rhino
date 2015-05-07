/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-383965.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 383965;
var summary = 'getter function with toSource';
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
printStatus (summary);
 
expect = /({get aaa :{}})|({aaa:{prototype:{}}})/;

getter function aaa(){};
var obj = {};
var gett = this.__lookupGetter__("aaa");
gett.__proto__ = obj;
obj.__defineGetter__("aaa", gett);
actual = obj.toSource();

reportMatch(expect, actual, summary);
