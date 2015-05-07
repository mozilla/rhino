/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-278873.js';
//-----------------------------------------------------------------------------
// testcase by Philipp Vogt <vogge@vlbg.dhs.org>
var BUGNUMBER = 278873;
var summary = 'Don\'t Crash';
var actual = 'Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
printStatus (summary);
 
function SwitchTest( input) {
  switch ( input ) {
  default:   break;
  case A:    break;
  }
}

printStatus(SwitchTest + '');

actual = 'No Crash';

reportCompare(expect, actual, summary);
