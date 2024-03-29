/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-375801.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 375801;
var summary = 'uneval should use "(void 0)" instead of "undefined"';
var actual = '';
var expect = '';


//-----------------------------------------------------------------------------
test();
//-----------------------------------------------------------------------------

function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);
 
  expect = '({a: (void 0)})'
  actual = uneval({a: undefined})
  compareSource(expect, actual, summary + ': uneval');

  expect = 'function() {({a: undefined});}';
  actual = (function() {({a: undefined});}).toString();
  compareSource(expect, actual, summary + ': toString');

  expect = '(function () {({a: undefined});})';
  actual = (function () {({a: undefined});}).toSource();
  compareSource(expect, actual, summary + ': toSource');

  exitFunc ('test');
}
