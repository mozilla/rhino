/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-376052.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 376052;
var summary = 'javascript.options.anonfunfix to allow function (){} expressions';
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

  if (typeof window != 'undefined')
  {
    print('Test skipped. anonfunfix not configurable in browser.');
    reportCompare(expect, actual, summary);
  }
  else
  {
    expect = 'No Error';
    try
    {
      eval('function () {1;}');
      actual = 'No Error';
    }
    catch(ex)
    {
      actual = ex + '';
    }
    reportCompare(expect, actual, summary + ': 1');

    options('anonfunfix');

    expect = 'No Error';
    try
    {
      eval('(function () {1;})');
      actual = 'No Error';
    }
    catch(ex)
    {
      actual = ex + '';
    }
    reportCompare(expect, actual, summary + ': 2');

    expect = 'SyntaxError: syntax error';
    try
    {
      eval('function () {1;}');
      actual = 'No Error';
    }
    catch(ex)
    {
      actual = ex + '';
    }
    reportCompare(expect, actual, summary + ': 3');

  }

  exitFunc ('test');
}
