/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-411279.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 411279;
var summary = 'let declaration as direct child of switch body block';
var actual = 'No Crash';
var expect = 'No Crash';


//-----------------------------------------------------------------------------
test();
//-----------------------------------------------------------------------------

function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);

  function f(x) 
  {
    var value = '';

    switch(x)
    {
    case 1:
      value = "1 " + y; 
      break; 
    case 2: 
      let y = 42;
      value = "2 " + y; 
      break; 
    default:
      value = "default " + y;
    }

    return value;
  }

  expect = 'function f(x) { var value = \'\'; switch (x) { '
    + 'case 1: value = "1 " + y;  break; '
    + 'case 2: let y = 42; value = "2 " + y;  break; ' 
    + 'default: value = "default " + y; } return value; }';

  actual = f + '';

  compareSource(expect, actual, summary);

  // This should throw an error because of the Temporal Dead Zone (TDZ), but it throws a ReferenceError for the moment
  expect = 'ReferenceError: "y" is not defined.';
  actual = "no exception";
  try { f(1) } catch (e) { actual = e.toString(); }
  reportCompare(expect, actual, summary + ': f(1)');

  expect = '2 42';
  actual = f(2);
  reportCompare(expect, actual, summary + ': f(2)');

  // This should throw another error because of the TDZ, but currently it returns some value
  expect = 'default 42';
  actual = f(3);
  reportCompare(expect, actual, summary + ': f(3)');
 
  exitFunc ('test');
}
