/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-462734-02.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 462734;
var summary = 'Do not assert: pobj_ == obj2';
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
printStatus (summary);

var save__proto__ = __proto__;

try
{
  for (x in function(){}) ([]);
  this.__defineGetter__("x", Function);
  __proto__ = x;
  prototype += [];
}
catch(ex)
{
  print(ex + '');
}

__proto__ = save__proto__;

reportCompare(expect, actual, summary);
