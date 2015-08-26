/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-351070-01.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 351070;
var summary = 'decompilation of let declaration should not change scope';
var summarytrunk = 'let declaration must be direct child of block or top-level implicit block';

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

  var f;
  var c;

  try
  {
    c = '(function () { var a = 2; if (!!true) let a = 3; return a; })';
    f = eval(c);
    expect = 'function () { var a = 2; if (!!true) let a = 3; return a; }';
    actual = f + '';
    compareSource(expect, actual, summary);

    expect = 3;
    actual = f();
    reportCompare(expect, actual, summary);
  }
  catch(ex)
  {
    // See https://bugzilla.mozilla.org/show_bug.cgi?id=408957
    expect = 'SyntaxError';
    actual = ex.name;
    reportCompare(expect, actual, summarytrunk + ': ' + c);
  }

  try
  {
    c = '(function () { var a = 2; if (!!true) {let a = 3;} return a; })';
    f = eval(c);
    expect = 'function () { var a = 2; if (!!true) { let a = 3;} return a; }';
    actual = f + '';
    compareSource(expect, actual, summary);

    expect = 2;
    actual = f();
    reportCompare(expect, actual, summary);
  }
  catch(ex)
  {
    // See https://bugzilla.mozilla.org/show_bug.cgi?id=408957
    expect = 'SyntaxError';
    actual = ex.name;
    reportCompare(expect, actual, summarytrunk + ': ' + c);
  }

  exitFunc ('test');
}
