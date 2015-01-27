/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-410852.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 410852;
var summary = 'Valgrind errors in jsemit.c';
var actual = '';
var expect = 'SyntaxError: syntax error';


//-----------------------------------------------------------------------------
test();
//-----------------------------------------------------------------------------

function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);
 
  print('Note: You must run this test under valgrind to determine if it passes');

  try
  {
    eval('function(){if(t)');
  }
  catch(ex)
  {
    actual = ex + '';
    print(actual);
  }

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
