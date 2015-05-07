/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-58116.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 58116;
var summary = 'Compute Daylight savings time correctly regardless of year';
var actual = '';
var expect = '';
var status;

printBugNumber(BUGNUMBER);
printStatus (summary);

expect = (new Date(2005, 7, 1).getTimezoneOffset());

status = summary + ' ' + inSection(1) + ' 1970-07-1 ';
actual = (new Date(1970, 7, 1).getTimezoneOffset());
reportCompare(expect, actual, status);
 
status = summary + ' ' + inSection(2) + ' 1965-07-1 ';
actual = (new Date(1965, 7, 1).getTimezoneOffset());
reportCompare(expect, actual, status);
 
status = summary + ' ' + inSection(3) + ' 0000-07-1 ';
actual = (new Date(0, 7, 1).getTimezoneOffset());
reportCompare(expect, actual, status);
