/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-375711.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 375711;
var summary = 'Do not assert with /[Q-b]/i.exec("")';
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
 
  var s;

  // see bug 416933
  print('see bug 416933 for changed behavior on Gecko 1.9');

  try
  {
    s = '/[Q-b]/.exec("")';
    expect = 'No Error';
    print(s + ' expect ' + expect);
    eval(s);
    actual = 'No Error';
  }
  catch(ex)
  {
    actual = ex + '';
  }
  reportCompare(expect, actual, summary + ': ' + s);

  try
  {
    s ='/[Q-b]/i.exec("")';
    expect = 'No Error';
    print(s + ' expect ' + expect);
    eval(s);
    actual = 'No Error';
  }
  catch(ex)
  {
    actual = ex + '';
  }
  reportCompare(expect, actual, summary + ': ' + s);

  try
  {
    s = '/[q-b]/.exec("")';
    expect = 'SyntaxError: invalid range in character class';
    print(s + ' expect ' + expect);
    eval(s);
    actual = 'No Error';
  }
  catch(ex)
  {
    actual = ex + '';
  }
  reportCompare(expect, actual, summary + ': ' + s);

  try
  {
    s ='/[q-b]/i.exec("")';
    expect = 'SyntaxError: invalid range in character class';
    print(s + ' expect ' + expect);
    eval(s);
    actual = 'No Error';
  }
  catch(ex)
  {
    actual = ex + '';
  }
  reportCompare(expect, actual, summary + ': ' + s);

  exitFunc ('test');
}
