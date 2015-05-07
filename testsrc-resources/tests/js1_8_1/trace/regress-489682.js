/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-489682.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 489682;
var summary = 'TM: wrong number with nested type-unstable loops';
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
printStatus (summary);

jit(true);

var v = 0;

for each (var a in [0, {}, {}, {}]) {
  print(v);
  v = v >>> 0;
  for each (var b in [{}, {}, new String(''), 42, new String(''), {}, 42]) 
           {
           }
         }
print(v);

jit(false);

expect = '0';
actual = v + '';

reportCompare(expect, actual, summary);
