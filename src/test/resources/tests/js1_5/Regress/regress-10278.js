/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-10278.js';
/**
 *  File Name:          regress-10278.js
 *  Reference:          https://bugzilla.mozilla.org/show_bug.cgi?id=10278
 *  Description:        Function declarations do not need to be separated
 *                      by semi-colon if they occur on the same line.
 *  Author:             bob@bclary.com
 */
//-----------------------------------------------------------------------------
var BUGNUMBER = 10278;
var summary = 'Function declarations do not need to be separated by semi-colon';
var actual;
var expect;


//-----------------------------------------------------------------------------
test();
//-----------------------------------------------------------------------------

function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);

  expect = 'pass';
  try
  {
    eval("function f(){}function g(){}");
    actual = "pass";
    printStatus('no exception thrown');
  }
  catch ( e )
  {
    actual = "fail";
    printStatus('exception ' + e.toString() + ' thrown');
  }

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
