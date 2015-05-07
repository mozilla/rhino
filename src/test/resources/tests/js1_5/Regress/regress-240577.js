/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-240577.js';
//-----------------------------------------------------------------------------
// originally reported by Jens Thiele <karme@unforgettable.com> in
var BUGNUMBER = 240577;
var summary = 'object.watch execution context';
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
printStatus (summary);

var createWatcher = function ( watchlabel )
{
  var watcher = function (property, oldvalue, newvalue)
  {
    actual += watchlabel; return newvalue;
  };
  return watcher;
};

var watcher1 = createWatcher('watcher1');

var object = {property: 'value'};

object.watch('property', watcher1);

object.property = 'newvalue';

expect = 'watcher1';

reportCompare(expect, actual, summary);
