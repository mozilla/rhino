/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-369696-02.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 396696;
var summary = 'Do not assert: map->depth > 0" in js_LeaveSharpObject';
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

  native = encodeURIComponent;
  n = native.prototype;
  n.__defineGetter__("prototype", n.toSource);
  p = n.__lookupGetter__("prototype");
  n = p;
  n["prototype"] = [n];
  n = p;
  p2 = n["prototype"];
  n = p2;
  n.__defineGetter__("0", n.toString);
  n = p;
  n();

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
