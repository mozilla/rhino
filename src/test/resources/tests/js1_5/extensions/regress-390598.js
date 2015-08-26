/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-390598.js';

//-----------------------------------------------------------------------------
var BUGNUMBER = 390598;
var summary = 'array_length_setter is exploitable';
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
 
  function exploit() {
    var fun = function () {};
    fun.__proto__ = [];
    fun.length = 0x50505050 >> 1;
    fun();
  }
  exploit();

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
