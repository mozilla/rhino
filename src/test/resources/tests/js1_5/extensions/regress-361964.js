/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-361964.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 361964;
var summary = 'Crash [@ MarkGCThingChildren] involving watch and setter';
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

  if (typeof document == 'undefined')
  {
    document = {};
  }

  if (typeof alert == 'undefined')
  {
    alert = print;
  }

// Crash:
  document.watch("title", function(a,b,c,d) {
		   return { toString : function() { alert(1); } };
		 });
  document.title = "xxx";

// No crash:
  document.watch("title", function() {
		   return { toString : function() { alert(1); } };
		 });
  document.title = "xxx";

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
