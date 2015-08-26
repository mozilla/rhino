/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-458851.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 458851;
var summary = 'TM: for-in loops should not skip every other value sometimes';
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
printStatus (summary);
 
jit(true);

function f() {
  var a = [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16];
  var x = 0;
  for (var i in a) {
    i = parseInt(i);
    x++;
  }
  print(actual = x);
}

expect = 16;
f();

jit(false);

reportCompare(expect, actual, summary);
