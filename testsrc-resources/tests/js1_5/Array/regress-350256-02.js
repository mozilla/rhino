/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-350256-02.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 350256;
var summary = 'Array.apply maximum arguments: 2^20';
var actual = '';
var expect = '';


//-----------------------------------------------------------------------------
test(Math.pow(2, 20));
//-----------------------------------------------------------------------------

function test(length)
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);
 

  var a = new Array();
  a[length - 2] = 'length-2';
  a[length - 1] = 'length-1';

  var b = Array.apply(null, a);

  expect = length + ',length-2,length-1';
  actual = b.length + "," + b[length - 2] + "," + b[length - 1];
  reportCompare(expect, actual, summary);

  function f() {
    return arguments.length + "," + arguments[length - 2] + "," +
      arguments[length - 1];
  }

  expect = length + ',length-2,length-1';
  actual = f.apply(null, a);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
