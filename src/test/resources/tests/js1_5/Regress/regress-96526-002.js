/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 *
 * Date:    04 Sep 2002
 * SUMMARY: Just seeing that we don't crash when compiling this script -
 *
 * See http://bugzilla.mozilla.org/show_bug.cgi?id=96526
 *
 */
//-----------------------------------------------------------------------------
var gTestfile = 'regress-96526-002.js';
printBugNumber(96526);
printStatus("Just seeing that we don't crash when compiling this script -");


/*
 * Tail recursion test by Georgi Guninski
 */
a="[\"b\"]";
s="g";
for(i=0;i<20000;i++)
  s += a;
try {eval(s);}
catch (e) {};

reportCompare('No Crash', 'No Crash', '');
