/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-371636.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 371636;
var summary = 'Numeric sort performance';
var actual = false;
var expect = '(tint/tstr < 3)=true';


//-----------------------------------------------------------------------------
test();
//-----------------------------------------------------------------------------

function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);
 
  function testint(power)
  {
    var N = 1 << power;
    var a = new Array(N);
    for (var i = 0; i != N; ++i)
      a[i] = (N-1) & (0x9E3779B9 * i);
    var now = Date.now;
    var t = now();
    a.sort();
    return now() - t;
  }

  function teststr(power)
  {
    var N = 1 << power;
    var a = new Array(N);
    for (var i = 0; i != N; ++i)
      a[i] = String((N-1) & (0x9E3779B9 * i));
    var now = Date.now;
    var t = now();
    a.sort();
    return now() - t;
  }

  var tint = testint(18);
  var tstr = teststr(18);
  print('int: ' + tint, 'str: ' + tstr, 'int/str: ' + (tint/tstr).toFixed(2));

  actual = '(tint/tstr < 3)=' + (tint/tstr < 3);
  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
