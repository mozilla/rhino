/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-359062.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 359062;
var summary = 'Access generator local variables from nested functions';
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
 
  expect = "Generator string";

  var scope = "Global";

  function gen() {
    var scope = "Generator";
    function inner() {
      actual = scope + " " + typeof scope;
    }
    inner();
    yield;
  }

  gen().next();

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
