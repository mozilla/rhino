/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-476257.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 476257;
var summary = 'Do not assert: !JS_ON_TRACE(cx)';
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
printStatus (summary);
 
jit(true);

function f1() {
  try
  {
    __proto__.functional getter = function ()
      {
        if (typeof gczeal == 'function') { gczeal(0); }
      }
    for each (let [[]] in [true, new Boolean(true), new Boolean(true)]) {}
  }
  catch(ex)
  {
    print(ex + '');
  }
}

function f2() {
  try
  {
    __proto__.functional getter = function () 
      { 
        if (typeof dis == 'function') { dis(); } 
      }
    for each (let [[]] in [true, new Boolean(true), new Boolean(true)]) {}
  }
  catch(ex)
  {
    print(ex + '');
  }
}

f1();
f2();

jit(false);

reportCompare(expect, actual, summary);
