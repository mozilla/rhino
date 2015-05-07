/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-349616.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 349616;
var summary = 'decompilation of getter keyword';
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

  var f;
  f = function() {
    window.foo getter = function() { return 5; };
    print(window.foo);
  }

  actual = f + '';
  expect = 'function () {\n    window.foo getter= ' +
    'function () {return 5;};\n    print(window.foo);\n}';

  compareSource(expect, actual, summary);

  exitFunc ('test');
}
