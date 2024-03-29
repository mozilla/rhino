/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-379245.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 379245;
var summary = 'inline calls';
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
 
  var fThis;

  function f()
  {
    fThis = this;
    return ({x: f}).x;
  }

  f()();

  if (this !== fThis)
    throw "bad this";

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
