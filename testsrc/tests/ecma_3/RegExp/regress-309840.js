/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-309840.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 309840;
var summary = 'Treat / in a literal regexp class as valid';
var actual = 'No error';
var expect = 'No error';

printBugNumber(BUGNUMBER);
printStatus (summary);

try
{ 
  var re = eval('/[/]/');
}
catch(e)
{
  actual = e.toString();
}

reportCompare(expect, actual, summary);
