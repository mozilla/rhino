/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-466262.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 466262;
var summary = 'Do not assert: f == f->root';
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

  jit(true);

  var e = 1;
  for (var d = 0; d < 3; ++d) {
    if (d == 2) {
      e = "";
    }
  }
  jit(false);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
