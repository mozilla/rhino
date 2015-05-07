/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-452498-160.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 452498;
var summary = 'TM: upvar2 regression tests';
var actual = '';
var expect = '';

//-------  Comment #160  From  Gary Kwong

//-----------------------------------------------------------------------------
test();
//-----------------------------------------------------------------------------

function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);

// Assertion failure: cg->upvars.lookup(atom), at ../jsemit.cpp:2034

  (function(){for(var x in (x::window = x for (x in []))[[]]){}})();
  reportCompare(expect, actual, summary + ': 1');

// crash [@ js_Interpret]
  (eval("(function(){ watch(\"x\", function () { new function ()y } ); const y });"))();
  x = NaN;
  reportCompare(expect, actual, summary + ': 2');

// Assertion failure: JOF_OPTYPE(op) == JOF_ATOM, at ../jsemit.cpp:5916
  ({ set z(){},  set y x()--x });
  reportCompare(expect, actual, summary + ': 3');

  exitFunc ('test');
}
