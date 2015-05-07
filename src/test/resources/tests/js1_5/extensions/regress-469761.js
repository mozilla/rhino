/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-469761.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 469761;
var summary = 'TM: Do not assert: JSVAL_IS_INT(STOBJ_GET_SLOT(callee_obj, JSSLOT_PRIVATE))';
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

  var o = { __proto__: function(){} };
  for (var j = 0; j < 3; ++j) { try { o.call(3); } catch (e) { } }

  jit(false);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
