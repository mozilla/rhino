/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = '15.3.4.4.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 290488;
var summary = '15.3.4.4 - Function.prototype.call() Scope';
var actual = '';
var expect = '';
var description = '';
var GLOBAL = this;

printBugNumber(BUGNUMBER);
printStatus (summary);

printStatus(inSection(1));
 
function func() { return this; }

description = 'top-level function: this == GLOBAL';
expect = GLOBAL;
actual = func.call();
reportCompare(expect, actual, description);

printStatus(inSection(2));

function getBoundMethod()
{
  return it.bindMethod("boundMethod", function () { return this; });
}

// it is a js shell only construction
if (typeof it != 'undefined')
{
  description = 'bound method: this == GLOBAL';
  var func = getBoundMethod();
  expect = GLOBAL;
  actual = func.call();
  reportCompare(expect, actual, description);
}
