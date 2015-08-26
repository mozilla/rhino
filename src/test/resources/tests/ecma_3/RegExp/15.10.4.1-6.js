/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
  15.10.4.1 new RegExp(pattern, flags)

  If F contains any character other than "g", "i", or" m", or if it
  contains the same one more than once, then throw a SyntaxError
  exception.
*/

var gTestfile = '15.10.4.1-6.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 476940;
var summary = 'Section 15.10.4.1 - RegExp with invalid flags';
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

  var invalidflags = ['ii', 'gg', 'mm', 'a'];

  for (var i = 0; i < invalidflags.length; i++)
  {
    var flag = invalidflags[i];
    expect = 'SyntaxError: invalid regular expression flag ' + flag.charAt(0);
    actual = '';
    try
    {
      new RegExp('bar', flag);
    }
    catch(ex)
    {
      actual = ex + '';
    }
    reportCompare(expect, actual, summary + 
                  ': new RegExp("bar", "' + flag + '")');

    actual = '';
    try
    {
      eval("/bar/" + flag);
    }
    catch(ex)
    {
      actual = ex + '';
    }
    reportCompare(expect, actual, summary + ': /bar/' + flag + ')');
  }

  exitFunc ('test');
}
