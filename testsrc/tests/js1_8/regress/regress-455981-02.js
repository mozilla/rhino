/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-455981-02.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 455981;
var summary = 'Do not assert: entry->localKind == JSLOCAL_ARG';
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

  expect = 'SyntaxError: duplicate argument is mixed with destructuring pattern';

  try
  {
    eval('(function ({a: {b: bb, c: cc, d: dd}, m: [x, n, o, p]}, x) {});');
  }
  catch(ex)
  {
    actual = ex + '';
  }
  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
