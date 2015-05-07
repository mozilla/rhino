/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-340526-01.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 340526;
var summary = 'Iterators: cross-referenced objects with close handler can ' +
  'delay close handler execution';
var actual = 'No Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
printStatus (summary);

try
{
  var iter = Iterator({});
  iter.foo = "bar";
  for (var i in iter)
    ;
}
catch(ex)
{
  print(ex + '');
}
 
reportCompare(expect, actual, summary);
