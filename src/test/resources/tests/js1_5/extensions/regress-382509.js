/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-382509.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 382509;
var summary = 'Disallow non-global indirect eval';
var actual = '';
var expect = '';

var global = typeof window == 'undefined' ? this : window;
var object = {};

//-----------------------------------------------------------------------------
test();
//-----------------------------------------------------------------------------

function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);

  if (options().match(/strict/))
  {
    options('strict');
  }
  if (options().match(/werror/))
  {
    options('werror');
  }

  global.foo = eval;
  global.a   = 'global';
  expect = 'global indirect';
  actual = global.foo('a+" indirect"');
  reportCompare(expect, actual, summary + ': global indirect');

  object.foo = eval;
  object.a   = 'local';
  expect = 'EvalError: function eval must be called directly, and not by way of a function of another name';
  try
  {
    actual = object.foo('a+" indirect"');
  }
  catch(ex)
  {
    actual = ex + '';
  }
  reportCompare(expect, actual, summary + ': local indirect');

  options('strict');
  options('werror');

  try
  {
    var foo = eval;
    print("foo(1+1)" + foo('1+1'));
    actual = 'No Error';
  }
  catch(ex)
  {
    actual = ex + '';
  }
  reportCompare(expect, actual, summary + ': strict, rename warning');

  options('strict');
  options('werror');

  expect = 'No Error';
  try
  {
    var foo = eval;
    foo('1+1');
    actual = 'No Error';
  }
  catch(ex)
  {
    actual = ex + '';
  }
  reportCompare(expect, actual, summary + ': not strict, no rename warning');

  exitFunc ('test');
}
