/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-360969-03.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 360969;
var summary = '2^17: local const';
var actual = 'No Crash';
var expect = 'No Crash';

var global = this;

//-----------------------------------------------------------------------------
test();
//-----------------------------------------------------------------------------

function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);
 
  var start = new Date();
  var p;
  var i;
  var limit = 2 << 16;

  for (var i = 0; i < limit; i++)
  {
    eval('const pv' + i + ';');
  }

  reportCompare(expect, actual, summary);

  var stop = new Date();

  print('Elapsed time: ' + Math.floor((stop - start)/1000) + ' seconds');

  exitFunc ('test');
}
