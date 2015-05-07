/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-346642-06.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 346642;
var summary = 'decompilation of destructuring assignment';
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

  expect = 3;
  actual = '';
  "" + function() { [] = 3 }; actual = 3;
  actual = 3;
  reportCompare(expect, actual, summary + ': 1');

  try
  {
    var z = 6;
    var f = eval('(function (){for(let [] = []; false;) let z; return z})');
    expect =  f();
    actual = eval("("+f+")")()
      reportCompare(expect, actual, summary + ': 2');
  }
  catch(ex)
  {
    // See https://bugzilla.mozilla.org/show_bug.cgi?id=408957
    var summarytrunk = 'let declaration must be direct child of block or top-level implicit block';
    expect = 'SyntaxError';
    actual = ex.name;
    reportCompare(expect, actual, summarytrunk);
  }

  expect = 3;
  actual = '';
  "" + function () { for(;; [[a]] = [5]) { } }; actual = 3;
  reportCompare(expect, actual, summary + ': 3');

  expect = 3;
  actual = '';
  f = function () { return { set x([a]) { yield; } } }
  var obj = f();
  uneval(obj); actual = 3;
  reportCompare(expect, actual, summary + ': 4');

  expect = 3;
  actual = '';
  "" + function () { [y([a]=b)] = z }; actual = 3;
  reportCompare(expect, actual, summary + ': 5');

  expect = 3;
  actual = '';
  "" + function () { for(;; ([[,]] = p)) { } }; actual = 3;
  reportCompare(expect, actual, summary + ': 6');

  expect = 3;
  actual = '';
  actual = 1; try {for(x in (function ([y]) { })() ) { }}catch(ex){} actual = 3;
  reportCompare(expect, actual, summary + ': 7');

  exitFunc ('test');
}
