/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-392310.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 392310;
var summary = 'send on newborn generator';
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

  print("See http://developer.mozilla.org/en/docs/New_in_JavaScript_1.7");

  function yielder() {
    actual = 'before yield';
    yield;
    actual = 'after yield';
  }

  expect = /TypeError: attempt to send ("before yield"|value) to newborn generator/i;
  var gen = yielder();
  try
  {
    gen.send('before yield');
  }
  catch(ex)
  {
    actual = ex + '';
  }
  reportMatch(expect, actual, 'send(value) to newborn generator');

  expect = 'before yield';
  var gen = yielder();
  gen.send(undefined);
  reportCompare(expect, actual, 'send(undefined) to newborn generator');

  exitFunc ('test');
}
