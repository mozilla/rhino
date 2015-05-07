/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-459293.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 459293;
var summary = 'Allow redeclaration of JSON';
var actual = '';
var expect = '';

  printBugNumber(BUGNUMBER);
  printStatus (summary);
 
  try
  {
    eval('var JSON = "foo";');
  }
  catch(ex)
  {
    actual = ex + '';
  }
  reportCompare(expect, actual, summary);
