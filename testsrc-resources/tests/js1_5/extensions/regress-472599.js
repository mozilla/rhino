/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-472599.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 472599;
var summary = 'Do not assert: JSVAL_IS_INT(STOBJ_GET_SLOT(callee_obj, JSSLOT_PRIVATE))';
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

  var a = (function(){}).prototype;
  a.__proto__ = a.toString;
  for (var i = 0; i < 4; ++i) { try{ a.call({}); } catch(e) { } }

  jit(false);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
