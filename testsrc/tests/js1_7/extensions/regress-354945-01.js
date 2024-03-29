/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-354945-01.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 354945;
var summary = 'Do not crash with new Iterator';
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
 
  var obj = {};
  obj.__iterator__ = function(){ };
  for(t in (new Iterator(obj))) { }

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
