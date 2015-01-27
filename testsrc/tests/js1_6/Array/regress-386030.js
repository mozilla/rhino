/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-386030.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 386030;
var summary = 'Array.reduce should ignore holes';
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
 
  function add(a, b) { return a + b; }
  function testreduce(v) { return v == 3 ? "PASS" : "FAIL"; }

  expect = 'PASS';

  try {
    a = new Array(2);
    a[1] = 3;
    actual = testreduce(a.reduce(add));
  } catch (e) {
    actual = "FAIL, reduce";
  }
  reportCompare(expect, actual, summary + ': 1');

  try {
    a = new Array(2);
    a[0] = 3;
    actual = testreduce(a.reduceRight(add));
  } catch (e) {
    actual = "FAIL, reduceRight";
  }
  reportCompare(expect, actual, summary + ': 2');

  try {
    a = new Array(2);
    a.reduce(add);
    actual = "FAIL, empty reduce";
  } catch (e) {
    actual = "PASS";
  }
  reportCompare(expect, actual, summary + ': 3');

  try {
    a = new Array(2);
    print(a.reduceRight(add));
    actual = "FAIL, empty reduceRight";
  } catch (e) {
    actual = "PASS";
  }
  reportCompare(expect, actual, summary + ': 4');

  exitFunc ('test');
}
