/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-366601.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 366601;
var summary = 'Switch with more than 64k atoms';
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
 
  var N = 100*1000;
  var src = 'var x = ["';
  var array = Array(N);
  for (var i = 0; i != N; ++i)
    array[i] = i;
  src += array.join('","')+'"];\n';
  src += 'switch (a) { case "a": case "b": case "c": return null; }  return x;';
  var f = Function('a', src);
  var r = f("a");
  if (r !== null)
    throw "Unexpected result: bad switch label";

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
