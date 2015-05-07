/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = '15.7.4.7-2.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = "411893";
var summary = "num.toPrecision(undefined) should equal num.toString()";
var actual, expect;

printBugNumber(BUGNUMBER);
printStatus(summary);

/**************
 * BEGIN TEST *
 **************/

var failed = false;

try
{
  var prec = 3.3.toPrecision(undefined);
  var str  = 3.3.toString();
  if (prec !== str)
  {
    throw "not equal!  " +
          "3.3.toPrecision(undefined) === '" + prec + "', " +
          "3.3.toString() === '" + str + "'";
  }
}
catch (e)
{
  failed = e;
}

expect = false;
actual = failed;

reportCompare(expect, actual, summary);
