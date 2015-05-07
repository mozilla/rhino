/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-348532.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 348532;
var summary = 'Do not overflow int when constructing Error.stack';
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

  expectExitCode(0);
  expectExitCode(3);

  actual = 0;
 
  // construct string of 1<<23 characters
  var s = Array((1<<23)+1).join('x');

  var recursionDepth = 0;
  function err() {
    if (++recursionDepth == 128)
      return new Error();
    return err.apply(this, arguments);
  }

  // The full stack trace in error would include 128*2 copies of s exceeding
  //  2^23 * 256 or 2^31 in length
  var error = err(s,s);

  print(error.stack.length);

  expect = true;
  actual = (error.stack.length > 0);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
