/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-353214-01.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 353214;
var summary = 'bug 353214';
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
 
  var f = function () {
    switch(({ get x() { export *; }, set x([[y], [x] ]) { let x; } })) {
      case eval("[[1]]", function(id) { return id; }):
      L:for(let x in (((eval).call)(eval("yield <x><y/></x>;",  "" ))))var x;
      case (uneval(this)):
      import x.*;
    }
  }

  expect = 'function () { ' +
    'switch({ get x() { export *; }, set x([[y], [x] ]) { let x; } }) {  ' +
    'case eval("[[1]]", function(id) { return id; }):  ' +
    'L:for(let x in eval.call(eval("yield <x><y/></x>;",  "" ))){var x;} ' +
    'case uneval(this):  ' +
    'import x.*;  default:;' +
    '} ' +
    '}';

  actual = f + '';

  compareSource(expect, actual, summary);

  exitFunc ('test');
}
