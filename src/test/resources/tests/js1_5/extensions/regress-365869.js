/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-365869.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 365869;
var summary = 'strict warning for object literal with duplicate propery names';
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

  if (!options().match(/strict/))
  {
    options('strict');
  }
  if (!options().match(/werror/))
  {
    options('werror');
  }

  try
  {
    expect = 'TypeError: redeclaration of property a';
    var o = {a:4, a:5};
    // syntax warning, need to eval to catch
    actual = 'No warning';
  }
  catch(ex)
  {
    actual = ex + '';
    print(ex);
  }
 
  reportCompare(expect, actual, summary);

  print('test crash from bug 371292 Comment 3');

  try
  {
    expect = 'TypeError: redeclaration of property 1';
    var o1 = {1:1, 1:2};
    // syntax warning, need to eval to catch
    actual = 'No warning';
  }
  catch(ex)
  {
    actual = ex + '';
    print(ex);
  }
 
  reportCompare(expect, actual, summary);


  print('test crash from bug 371292 Comment 9');

  try
  {
    expect = 'TypeError: redeclaration of const 5';
    "012345".__defineSetter__(5, function(){});
  }
  catch(ex)
  {
    actual = ex + '';
    print(ex);
  }

  reportCompare(expect, actual, summary);


  exitFunc ('test');
}
