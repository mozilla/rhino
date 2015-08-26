/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-385393-05.js';

//-----------------------------------------------------------------------------
var BUGNUMBER = 385393;
var summary = 'Regression test for bug 385393';
var actual = 'No Crash';
var expect = 'No Crash';

//-----------------------------------------------------------------------------
test();
//-----------------------------------------------------------------------------

function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);
 
  function c(gen)
  {
    Iterator;    

    "" + gen;

    for (var i in gen())
      ;
  }

  function gen()
  {
    ({}).hasOwnProperty();
    yield;
  }

  c(gen);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
