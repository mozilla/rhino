/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-452498-102.js';
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
 
// ------- Comment #102 From Gary Kwong [:nth10sd]

// =====

  (function(){function x(){} function x()y})();

// Assertion failure: JOF_OPTYPE(op) == JOF_ATOM, at ../jsemit.cpp:1710

// =====
  function f() {
    "" + (function(){
        for( ; [function(){}] ; x = 0)
          with({x: ""})
            const x = []
            });
  }
  f();

// Assertion failure: ss->top - saveTop <= 1U, at ../jsopcode.cpp:2156

// =====

  try
  {
    function f() {
      var x;
      eval("const x = [];");
    }
    f();
  }
  catch(ex)
  {
  }
// Assertion failure: regs.sp == StackBase(fp), at ../jsinterp.cpp:2984

// =====
  try
  {
    do {x} while([[] for (x in []) ]);
  }
  catch(ex)
  {
  }
// Assertion failure: !(pnu->pn_dflags & PND_BOUND), at ../jsemit.cpp:1818
// =====

  try
  {
    {x} ((x=[] for (x in []))); x;
  }
  catch(ex)
  {
  }
// Assertion failure: cg->staticLevel >= level, at ../jsemit.cpp:2014
// Crash [@ BindNameToSlot] in opt without -j

// =====

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}



