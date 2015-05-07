/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-422592.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 422592;
var summary = 'js.c dis/dissrc should not kill script execution';
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

  if (typeof dis == 'undefined')
  {
    expect = actual = 'Test requires function dis. Not tested';
    print(expect);
  }
  else
  {
    expect = 'Completed';
    actual = 'Not Completed';
    print('Before dis');
    try
    {
      dis(print);
    }
    catch(ex)
    {
      print(ex + '');
    }
    print('After dis');
    actual = 'Completed';
  }
  reportCompare(expect, actual, summary);

  if (typeof dissrc == 'undefined')
  {
    expect = actual = 'Test requires function dissrc. Not tested';
    print(expect);
  }
  else
  {
    print('Before dissrc');
    expect = 'Completed';
    actual = 'Not Completed';
    try
    {
      dissrc(print);
    }
    catch(ex)
    {
      print(ex + '');
    }
    print('After dissrc');
    actual = 'Completed';
  }
  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
