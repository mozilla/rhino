/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-416601.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 416601;
var summary = 'Property cache can be left disabled after exit from a generator or trap handler';
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
 
  function f()
  {
    with (Math) {
      yield 1;
    }
  }

  var iter = f();
  iter.next();
  iter.close();

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
