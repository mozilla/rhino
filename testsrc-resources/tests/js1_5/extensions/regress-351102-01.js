/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-351102-01.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 351102;
var summary = 'try/catch-guard/finally GC issues';
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
 
  var f;

  f = function () {
    try {
      throw new Error('bad');
    } catch (e if (e = null, gc(), false)) {
    } catch (e) {
      // e is dangling now
    }
  };

  f();

  reportCompare(expect, actual, summary + ': 1');

  exitFunc ('test');
}
