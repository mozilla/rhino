/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-355341.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 355341;
var summary = 'Do not crash with watch and setter';
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
 
  this.x setter= Function; this.watch('x', function () { }); x = 3;

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
