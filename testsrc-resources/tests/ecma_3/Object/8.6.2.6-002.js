/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = '8.6.2.6-002.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 470364;
var summary = '[[DefaultValue]] should not call valueOf, toString with an argument';
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

  expect = actual = 'No exception';

  try
  { 
    var o = { 
    valueOf: function() 
    { 
        if (arguments.length !== 0) 
          throw "unexpected arguments!  arg1 type=" + typeof arguments[0] + ", value=" +
            arguments[0]; 
        return 2; 
    } 
    };
    o + 3;
  }
  catch(ex)
  {
    actual = ex + '';
  }

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
