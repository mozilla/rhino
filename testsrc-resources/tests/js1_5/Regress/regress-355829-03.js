/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-355829-03.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 355829;
var summary = 'js_ValueToObject should return the original object if OBJ_DEFAULT_VALUE returns a primitive value';
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

  var a = [ { valueOf: function() { return null; } } ];
  a.toLocaleString();

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
