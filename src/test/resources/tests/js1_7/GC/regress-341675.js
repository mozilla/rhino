/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-341675.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 341675;
var summary = 'Iterators: still infinite loop during GC';
var actual = 'No Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
printStatus (summary);

var globalToPokeGC = {};

function generator()
{
  try {
    yield [];
  } finally {
    make_iterator();
  }
}

function make_iterator()
{
  var iter = generator();
  iter.next();
}

make_iterator();
gc();

reportCompare(expect, actual, summary);
