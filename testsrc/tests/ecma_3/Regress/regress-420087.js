/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-420087.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 420087;
var summary = 'Do not assert:  PCVCAP_MAKE(sprop->shape, 0, 0) == entry->vcap';
var actual = 'No Crash';
var expect = 'No Crash';


//-----------------------------------------------------------------------------
test();
//-----------------------------------------------------------------------------

function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);

  var dict;

  for (var i = 0; i < 2; i++)
    dict = {p: 1, q: 1, p:1};

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
