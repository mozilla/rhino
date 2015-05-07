/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-381108.js';

//-----------------------------------------------------------------------------
var BUGNUMBER = 381108;
var summary = 'decompilation of object literal should have space following :';
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
 
  var f = (function() { return {a:3, b getter: f} });
  expect = true;
  actual = /a: 3, b getter: f/.test(f + '');

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
