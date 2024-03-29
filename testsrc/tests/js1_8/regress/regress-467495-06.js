/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-467495-06.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 467495;
var summary = 'TCF_FUN_CLOSURE_VS_VAR is necessary';
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

  function f(x)
  {
    var y = 1;
    if (Math)
      function x() { }
    if (Math)
      function y() { }
    return [x, y];
  }

  var r = f(0);

  if (typeof(r[0]) != "function")
    actual += "Bad r[0]";

  if (typeof(r[1]) != "function")
    throw "Bad r[1]";

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
