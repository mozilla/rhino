/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-368516.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 368516;
var summary = 'Treat unicode BOM characters as whitespace';
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
 
  var bomchars = ['\uFFFE',
                  '\uFEFF'];

  for (var i = 0; i < bomchars.length; i++)
  {
    expect = 'howdie';
    actual = '';

    try
    {
      eval("var" + bomchars[i] + "hithere = 'howdie';");
      actual = hithere;
    }
    catch(ex)
    {
      actual = ex + '';
    }

    reportCompare(expect, actual, summary + ': ' + i);
  }

  exitFunc ('test');
}
