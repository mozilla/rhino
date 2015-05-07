/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 *
 * Date:    29 Sep 2003
 * SUMMARY: Testing __parent__ and __proto__ of Script object
 *
 * See http://bugzilla.mozilla.org/show_bug.cgi?id=220584
 */
//-----------------------------------------------------------------------------
var gTestfile = 'regress-220584.js';
var UBound = 0;
var BUGNUMBER = 220584;
var summary = 'Testing __parent__ and __proto__ of Script object';
var status = '';
var statusitems = [];
var actual = '';
var actualvalues = [];
var expect= '';
var expectedvalues = [];
var s;


// invoke |Script| as a function
status = inSection(1);
if (typeof Script == 'undefined')
{
  reportCompare("Script not defined, Test skipped.",
                "Script not defined, Test skipped.",
                summary);
}
else
{
  s = Script('1;');
  actual = s instanceof Object;
  expect = true;
  addThis();

  status = inSection(2);
  actual = (s.__parent__ == undefined) || (s.__parent__ == null);
  expect = false;
  addThis();

  status = inSection(3);
  actual = (s.__proto__ == undefined) || (s.__proto__ == null);
  expect = false;
  addThis();

  status = inSection(4);
  actual = (s + '').length > 0;
  expect = true;
  addThis();

}

// invoke |Script| as a constructor
status = inSection(5);
if (typeof Script == 'undefined')
{
  print('Test skipped. Script not defined.');
}
else
{
  s = new Script('1;');

  actual = s instanceof Object;
  expect = true;
  addThis();

  status = inSection(6);
  actual = (s.__parent__ == undefined) || (s.__parent__ == null);
  expect = false;
  addThis();

  status = inSection(7);
  actual = (s.__proto__ == undefined) || (s.__proto__ == null);
  expect = false;
  addThis();

  status = inSection(8);
  actual = (s + '').length > 0;
  expect = true;
  addThis();
}

//-----------------------------------------------------------------------------
test();
//-----------------------------------------------------------------------------

function addThis()
{
  statusitems[UBound] = status;
  actualvalues[UBound] = actual;
  expectedvalues[UBound] = expect;
  UBound++;
}


function test()
{
  enterFunc('test');
  printBugNumber(BUGNUMBER);
  printStatus(summary);

  for (var i=0; i<UBound; i++)
  {
    reportCompare(expectedvalues[i], actualvalues[i], statusitems[i]);
  }

  exitFunc ('test');
}
