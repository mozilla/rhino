/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-352455.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 352455;
var summary = 'Eval object with non-function getters/setters';
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

  print('If the test harness fails on this bug, the test fails.');
 
  expect = 'SyntaxError: invalid getter usage';
  z = ({});
  try { eval('z.x getter= /g/i;'); } catch(ex) { actual = ex + '';}
  print("This line should not be the last output you see.");
  try { print(uneval(z)); } catch(e) { print("Threw!"); print(e); }

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
