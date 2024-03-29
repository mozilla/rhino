/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-469239-01.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 469239;
var summary = 'TM: Do not assert: entry->kpc == (jsbytecode*) atoms[index]';
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

  for (let b=0;b<9;++b) {
    for each (let h in ['', 3, /x/]) {
	    for each (c in [[], [], [], /x/]) {
        '' + c;
	    }
    }
  }

  jit(false);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
