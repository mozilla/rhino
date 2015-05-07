/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-306633.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 306633;
var summary = 'report compile warnings in evald code when strict warnings enabled';
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
printStatus (summary);

if (!options().match(/strict/))
{
  options('strict');
}
if (!options().match(/werror/))
{
  options('werror');
}

expect = 'SyntaxError';

try
{
  actual = eval('super = 5');
}
catch(e)
{
  actual = e.name;
}

reportCompare(expect, actual, summary);
