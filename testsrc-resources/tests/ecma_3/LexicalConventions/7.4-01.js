/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = '7.4-01.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 475834;
var summary = ' /**/ comments with newlines in them do not act as line breaks';
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
 
  function f() {
  L:  for (var i=0; i<2; i++) {
      for (var j=0; j<2; j++) {
        break/*
              */L;
      }
      return "conformant!";
    }
    return "non-conformant!";
  }

  expect = 'conformant!';
  print(actual = f());

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
