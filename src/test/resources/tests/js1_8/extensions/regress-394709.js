/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-394709.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 394709;
var summary = 'Do not leak with object.watch and closure';
var actual = 'No Leak';
var expect = 'No Leak';

if (typeof countHeap == 'undefined')
{
  countHeap = function () { 
    print('This test requires countHeap which is not supported'); 
    return 0;
  };
}

//-----------------------------------------------------------------------------
test();
//-----------------------------------------------------------------------------

function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);

  runtest();
  gc();
  var counter = countHeap();
  runtest();
  gc();
  if (counter != countHeap())
    throw "A leaky watch point is detected";

  function runtest () {
    var obj = { b: 0 };
    obj.watch('b', watcher);

    function watcher(id, old, value) {
      ++obj.n;
      return value;
    }
  }

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
