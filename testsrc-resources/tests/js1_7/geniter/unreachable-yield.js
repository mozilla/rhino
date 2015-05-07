/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'unreachable-yield.js';
//-----------------------------------------------------------------------------
var BUGNUMBER     = "(none)";
var summary = "Iterator with unreachable yield statement";
var actual, expect;

printBugNumber(BUGNUMBER);
printStatus(summary);

/**************
 * BEGIN TEST *
 **************/

function gen()
{
  // this is still a generator even if yield can't
  // be invoked, because yield is a syntactical
  // part of the language
  if (false)
    yield "failed";
}

var failed = false;
try
{
  var it = gen();
  if (it == undefined)
    throw "gen() not recognized as generator";

  // no yields to execute
  var stopPassed = false;
  try
  {
    it.next();
  }
  catch (e)
  {
    if (e === StopIteration)
      stopPassed = true;
  }

  if (!stopPassed)
    throw "incorrect or invalid StopIteration";
}
catch (e)
{
  failed = e;
}

expect = false;
actual = failed;

reportCompare(expect, actual, summary);
