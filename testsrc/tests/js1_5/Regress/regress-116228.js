/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-116228.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 116228;
var summary = 'Do not crash - JSOP_THIS should null obj register';
var actual = 'No Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
printStatus (summary);

var obj = {};
obj.toString = function() {return this();}
  try
  {
    obj.toString();
  }
  catch(e)
  {
  }
reportCompare(expect, actual, summary);
