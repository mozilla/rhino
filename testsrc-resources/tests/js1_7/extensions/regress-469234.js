/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-469234.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 469234;
var summary = 'TM: Do not assert: !JS_ON_TRACE(cx)';
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

  for(var j=0;j<3;++j)({__proto__:[]}).__defineSetter__('x',function(){});

  jit(false);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
