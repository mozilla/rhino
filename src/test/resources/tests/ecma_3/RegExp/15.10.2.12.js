/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = '15.10.2.12.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 378738;
var summary = '15.10.2.12 - CharacterClassEscape \d';
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
 
  expect = false;
  actual = /\d/.test("\uFF11");

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
