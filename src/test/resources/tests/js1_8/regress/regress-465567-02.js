/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-465567-02.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 465567;
var summary = 'TM: Do not assert: JSVAL_TAG(v) == JSVAL_OBJECT';
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
printStatus (summary);

jit(true);

try
{
  eval("for each (e in ['', true, 1, true, 1]) { e = null; if (0) { let e; var e; } }");
}
catch(ex)
{
}

jit(false);

reportCompare(expect, actual, summary);
