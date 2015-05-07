/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-350704.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 350704;
var summary = 'decompilation of let nested in for';
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

  f = function() { try{} catch(y) { for(z(let(y=3)4); ; ) ; } }
  expect = 'function () {\n    try {\n    } catch (y) {\n        ' +
    'for (z(let (y = 3) 4);;) {\n        }\n    }\n}'
    actual = f + '';
  compareSource(expect, actual, summary);

  exitFunc ('test');
}
