/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'for-in.js';
//-----------------------------------------------------------------------------

var summary = "Contention among threads enumerating properties";
// Adapted from mozilla/js/src/xpconnect/tests/js/old/threads.js

printStatus (summary);

var LOOP_COUNT = 1000;
var THREAD_COUNT = 10;

var foo;
var bar;

function makeWorkerFn(id) {
  return function() {
    foo = id + 1;
    bar[id] = {p: 0};
    var n, m;
    for (n in bar) {
      for (m in bar[n]) {}
    }
    for (n in {}.__parent__) {}
  };
}

function range(n) {
  for (let i = 0; i < n; i++)
    yield i;
}

var expect;
var actual;

expect = actual = 'No crash';
if (typeof scatter == 'undefined') {
  print('Test skipped. scatter not defined.');
} else if (!("__parent__" in {})) {
  print('Test skipped. __parent__ not defined.');
} else {
  for (let i = 0; i < LOOP_COUNT; i++) {
    foo = 0;
    bar = new Array(THREAD_COUNT);
    scatter([makeWorkerFn(j) for (j in range(THREAD_COUNT))]);
  }
}

reportCompare(expect, actual, summary);
