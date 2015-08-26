/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-310295.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 310295;
var summary = 'Do not crash on JS_ValueToString';
var actual = 'No Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
printStatus (summary);

var tmp = 23948730458647527874392837439299837412374859487593;

tmp = new Number(tmp);
tmp = tmp.valueOf()
  tmp = String(tmp);
 
reportCompare(expect, actual, summary);
