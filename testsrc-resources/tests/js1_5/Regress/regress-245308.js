/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-245308.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 245308;
var summary = 'Don\'t Crash with nest try catch';
var actual = 'Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
printStatus (summary);

function foo(e) {
  try {}
  catch(ex) {
    try {}
    catch(ex) {}
  }
}

foo('foo');

actual = 'No Crash';
 
reportCompare(expect, actual, summary);
