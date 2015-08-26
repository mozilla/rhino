/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-474935.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 474935;
var summary = 'Do not assert: !ti->typeMap.matches(ti_other->typeMap)';
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

  var a = ["", 0, 0, 0, 0, 0, "", "", 0, "", 0, ""];
  var i = 0;
  var g = 0;
  for each (let e in a) {
      "" + [e];
      if (i == 3 || i == 7) {
        for each (g in [1]) {
          }
      }
      ++i;
    }

  jit(false);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
