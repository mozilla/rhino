/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-407957.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 407957;
var summary = 'Iterator is mutable.';
var actual = '';
var expect = '';


//-----------------------------------------------------------------------------
test();
//-----------------------------------------------------------------------------

function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);
 
  var obj           = {};
  var saveIterator  = Iterator;

  Iterator = obj;
  reportCompare(obj, Iterator, summary);

  exitFunc ('test');
}
