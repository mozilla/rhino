/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-422269.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 422269;
var summary = 'Compile-time let block should not capture runtime references';
var actual = 'No leak';
var expect = 'No leak';


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
    let m = {sin: Math.sin};
    (function() { m.sin(1); })();
    return m;
  }

  if (typeof countHeap == 'undefined')
  {
    expect = actual = 'Test skipped';
    print('Test skipped. Requires countHeap function.');
  }
  else
  {
    var x = f();
    gc();
    var n = countHeap();
    x = null;
    gc();

    var n2 = countHeap();
    if (n2 >= n)
      actual = "leak is detected, something roots the result of f";
  }
  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
