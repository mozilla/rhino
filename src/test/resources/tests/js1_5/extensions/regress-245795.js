/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-245795.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 245795;
var summary = 'eval(uneval(function)) should be round-trippable';
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
printStatus (summary);

if (typeof uneval != 'undefined')
{
  function a()
  {
    b=function() {};
  }

  var r = /function a\(\) \{ b = \(?function \(\) \{\s*\}\)?; \}/;
  eval(uneval(a));

  var v = a.toString().replace(/[ \n]+/g, ' ');
 
  printStatus("[" + v + "]");

  expect = true;
  actual = r.test(v);

  reportCompare(expect, actual, summary);
}
