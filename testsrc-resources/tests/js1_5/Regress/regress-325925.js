/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-325925.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 325925;
var summary = 'Do not Assert: c <= cs->length in AddCharacterToCharSet';
var actual = 'No Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
printStatus (summary);

/[\cA]/('\x01');
 
reportCompare(expect, actual, summary);
