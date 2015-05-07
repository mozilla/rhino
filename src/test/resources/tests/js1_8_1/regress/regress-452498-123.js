/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-452498-123.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 452498;
var summary = 'TM: upvar2 regression tests';
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


// ------- Comment #123 From Gary Kwong [:nth10sd]

// Does not require -j:
// =====
  try
  {
    eval('y = (function (){y} for (x in []);');
  }
  catch(ex)
  {
  }

// Assertion failure: !(pn->pn_dflags & flag), at ../jsparse.h:651
// =====
  (function(){for(var x = arguments in []){} function x(){}})();

// Assertion failure: dn->pn_defn, at ../jsemit.cpp:1873
// =====


// Requires -j:
// =====
  (function(){ eval("for (x in ['', {}, '', {}]) { this; }" )})();

// Assertion failure: fp->thisp == JSVAL_TO_OBJECT(fp->argv[-1]), at ../jstracer.cpp:4172
// =====
  for each (let x in ['', '', '']) { (new function(){} )}

// Assertion failure: scope->object == ctor, at ../jsbuiltins.cpp:236
// Opt crash [@ js_FastNewObject] near null
// =====

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
