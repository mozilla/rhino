/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-345879-01.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 345879;
var summary = 'Crash when calling a function from a generator with less arguments than its arity ';
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
 
  function gen() {
    yield isNaN();
  }

  f = gen();
  f.next();

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
