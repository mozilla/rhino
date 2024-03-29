/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-427196-03.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 427196;
var summary = 'Do not assert: OBJ_SCOPE(pobj)->object == pobj';
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

  var c = {__proto__: []};
  var a = {__proto__: {__proto__: {}}};
  c.hasOwnProperty;
  c.__proto__ = a;
  c.hasOwnProperty;

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
