/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-338121-03.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 338121;
var summary = 'Issues with JS_ARENA_ALLOCATE_CAST';
var actual = 'No Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
printStatus (summary);

try
{
  var fe="vv";

  for (i=0; i<24; i++)
    fe += fe;

  var fu=new Function(
    fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe,
    fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe,
    fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe, fe,
    fe, fe, fe,
    "done"
    );

//alert("fu="+fu);
//print("fu="+fu);
  var fuout = 'fu=' + fu;
}
catch(ex)
{
  expect = 'InternalError: script stack space quota is exhausted';
  actual = ex + '';
  print('Caught ' + ex);
}
print('Done');

reportCompare(expect, actual, summary);
