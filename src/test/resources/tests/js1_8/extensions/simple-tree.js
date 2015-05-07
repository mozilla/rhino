/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'simple-tree.js';
//-----------------------------------------------------------------------------

var summary = "Create a tree of threads";

var N = 50;  // number of threads to create

printStatus (summary);

function range(start, stop) {
  var a = [];
  for (var i = start; i < stop; i++)
    a.push(i);
  return a;
}

function tree(start, stop) {
  sleep(0.001);

  if (start >= stop)
    return [];
  else if (start + 1 >= stop)
    return [start];

  sleep(0.001);

  let mid = start + Math.floor((stop - start) / 2);
  let halves = scatter([function () { return tree(start, mid); },
                        function () { return tree(mid, stop); }]);
  sleep(0.001);
  return Array.prototype.concat.apply([], halves);
}

var expect;
var actual;

if (typeof scatter == 'undefined' || typeof sleep == 'undefined') {
  print('Test skipped. scatter or sleep not defined.');
  expect = actual = 'Test skipped.';
} else {
  expect = range(0, N).toSource();
  actual = tree(0, N).toSource();
}

reportCompare(expect, actual, summary);
