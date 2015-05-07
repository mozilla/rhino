/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-366941.js';

//-----------------------------------------------------------------------------
var BUGNUMBER = 366941;
var summary = 'Destructuring enumerations, iterations';
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

  var list1 = [[1,2],[3,4],[5,6]];
  var list2 = [[1,2,3],[4,5,6],[7,8,9]];

  expect = '1,2;3,4;5,6;';
  actual = '';

  for each (var [foo, bar] in list1) {
    actual += foo + "," + bar + ";";
  }

  reportCompare(expect, actual, summary + ': 1');

  expect = '1,2,3;4,5,6;7,8,9;';
  actual = '';
  for each (var [foo, bar, baz] in list2) {
    actual += foo + "," + bar + "," + baz + ";";
  }

  reportCompare(expect, actual, summary + ': 2');

  function gen(list) {
    for each (var test in list) {
      yield test;
    }
  }

  var iter1 = gen(list1);

  expect = '1,2;3,4;5,6;';
  actual = '';

  for (var [foo, bar] in iter1) {
    actual += foo + "," + bar + ";";
  }

  reportCompare(expect, actual, summary + ': 3');

  var iter2 = gen(list2);
  expect = '1,2,3;4,5,6;7,8,9;';
  actual = '';

  for (var [foo, bar, baz] in iter2) {
    actual += foo + "," + bar + "," + baz + ";";
  }

  reportCompare(expect, actual, summary + ': 4');

  exitFunc ('test');
}
