/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-356106.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 356106;
var summary = "Do not assert: rval[strlen(rval)-1] == '}'";
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
 
  (function() { return ({x setter: function(){} | 5 }) });

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
