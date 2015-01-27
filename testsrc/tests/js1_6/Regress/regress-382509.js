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

  global.foo = eval;
  global.a   = 'global';
  expect = 'global indirect';
  actual = String(['a+" indirect"'].map(global.foo));
  reportCompare(expect, actual, summary + ': global indirect');

  object.foo = eval;
  object.a   = 'local';
  expect = 'EvalError: function eval must be called directly, and not by way of a function of another name';
  try
  {
    actual = String(['a+" indirect"'].map(object.foo, object));
  }
  catch(ex)
  {
    actual = ex + '';
  }
  reportCompare(expect, actual, summary + ': local indirect');

  exitFunc ('test');
}
