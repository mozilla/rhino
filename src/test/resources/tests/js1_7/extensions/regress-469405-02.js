/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-469405-02.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 469405;
var summary = 'Do not assert: !JSVAL_IS_PRIMITIVE(regs.sp[-2])';
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
printStatus (summary);

try
{ 
  eval("__proto__.__iterator__ = [].toString");
  for (var z = 0; z < 3; ++z) { if (z % 3 == 2) { for(let y in []); } }
}
catch(ex)
{
  print('caught ' + ex);
}

reportCompare(expect, actual, summary);
