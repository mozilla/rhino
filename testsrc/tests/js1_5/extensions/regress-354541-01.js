/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-354541-01.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 354541;
var summary = 'Regression to standard class constructors in case labels';
var actual = '';
var expect = '';


printBugNumber(BUGNUMBER);
printStatus (summary + ': top level');

String.prototype.trim = function() { print('hallo'); };

const S = String;
const Sp = String.prototype;

expect = 'No Error';
actual = 'No Error';

if (typeof Script == 'undefined')
{
  print('Test skipped. Script not defined.');
}
else
{
  var s = Script('var tmp = function(o) { switch(o) { case String: case 1: return ""; } }; print(String === S); print(String.prototype === Sp); "".trim();');
  s();
}
 
reportCompare(expect, actual, summary);
