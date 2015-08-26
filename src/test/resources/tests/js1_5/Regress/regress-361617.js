/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-361617.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 361617;
var summary = 'Do not crash with getter, watch and gc';
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
 
  (function() { this.x getter= function(){} })();
  this.watch('x', print);
  this.x getter= function(){};
  gc();
  this.unwatch('x');
  x;

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
