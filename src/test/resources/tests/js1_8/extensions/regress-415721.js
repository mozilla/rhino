/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-415721.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 415721;
var summary = 'jsatom.c double hashing re-validation logic is unsound';

printStatus (summary);

var VARS = 10000;
var TRIES = 100;

function atomizeStressTest() {
  var fn = "function f() {var ";
  for (var i = 0; i < VARS; i++)
    fn += '_f' + i + ', ';
  fn += 'q;}';

  function e() { eval(fn); }

  for (var i = 0; i < TRIES; i++) {
    scatter([e,e]);
    gc();
  }
}

var expect;
var actual;

expect = actual = 'No crash';
if (typeof scatter == 'undefined' || typeof gc == 'undefined') {
  print('Test skipped. scatter or gc not defined.');
  expect = actual = 'Test skipped.';
} else {
  atomizeStressTest();
}

reportCompare(expect, actual, summary);

