/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-479381.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 479381;
var summary = 'Do not crash @ js_FinalizeStringRT with multi-threads.';
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

  if (typeof gczeal != 'function' || typeof scatter != 'function')
  {
    print(expect = actual = 'Test skipped: requires mulithreads');
  }
  else
  {
    expect = actual = 'No Crash';

    gczeal(2);

    function f() {
      var s;
      for (var i = 0; i < 9999; i++)
        s = 'a' + String(i)[3] + 'b';
      return s;
    }

    print(scatter([f, f, f, f]));
  }

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
