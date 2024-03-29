/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Date: 29 October 2001
 *
 * SUMMARY: Regression test for bug 107138
 * See http://bugzilla.mozilla.org/show_bug.cgi?id=107138
 *
 * The bug: arr['1'] == undefined instead of arr['1'] == 'one'.
 * The bug was intermittent and did not always occur...
 *
 * The cnSTRESS constant defines how many times to repeat this test.
 */
//-----------------------------------------------------------------------------
var gTestfile = 'regress-107138.js';
var UBound = 0;
var cnSTRESS = 10;
var cnDASH = '-';
var BUGNUMBER = 107138;
var summary = 'Regression test for bug 107138';
var status = '';
var statusitems = [];
var actual = '';
var actualvalues = [];
var expect= '';
var expectedvalues = [];


var arr = ['zero', 'one', 'two', 'three', 'four', 'five',
           'six', 'seven', 'eight', 'nine', 'ten'];


// This bug was intermittent. Stress-test it.
for (var j=0; j<cnSTRESS; j++)
{
  status = inSection(j + cnDASH + 1);
  actual = arr[0];
  expect = 'zero';
  addThis();

  status = inSection(j + cnDASH + 2);
  actual = arr['0'];
  expect = 'zero';
  addThis();

  status = inSection(j + cnDASH + 3);
  actual = arr[1];
  expect = 'one';
  addThis();

  status = inSection(j + cnDASH + 4);
  actual = arr['1'];
  expect = 'one';
  addThis();

  status = inSection(j + cnDASH + 5);
  actual = arr[2];
  expect = 'two';
  addThis();

  status = inSection(j + cnDASH + 6);
  actual = arr['2'];
  expect = 'two';
  addThis();

  status = inSection(j + cnDASH + 7);
  actual = arr[3];
  expect = 'three';
  addThis();

  status = inSection(j + cnDASH + 8);
  actual = arr['3'];
  expect = 'three';
  addThis();

  status = inSection(j + cnDASH + 9);
  actual = arr[4];
  expect = 'four';
  addThis();

  status = inSection(j + cnDASH + 10);
  actual = arr['4'];
  expect = 'four';
  addThis();

  status = inSection(j + cnDASH + 11);
  actual = arr[5];
  expect = 'five';
  addThis();

  status = inSection(j + cnDASH + 12);
  actual = arr['5'];
  expect = 'five';
  addThis();

  status = inSection(j + cnDASH + 13);
  actual = arr[6];
  expect = 'six';
  addThis();

  status = inSection(j + cnDASH + 14);
  actual = arr['6'];
  expect = 'six';
  addThis();

  status = inSection(j + cnDASH + 15);
  actual = arr[7];
  expect = 'seven';
  addThis();

  status = inSection(j + cnDASH + 16);
  actual = arr['7'];
  expect = 'seven';
  addThis();

  status = inSection(j + cnDASH + 17);
  actual = arr[8];
  expect = 'eight';
  addThis();

  status = inSection(j + cnDASH + 18);
  actual = arr['8'];
  expect = 'eight';
  addThis();

  status = inSection(j + cnDASH + 19);
  actual = arr[9];
  expect = 'nine';
  addThis();

  status = inSection(j + cnDASH + 20);
  actual = arr['9'];
  expect = 'nine';
  addThis();

  status = inSection(j + cnDASH + 21);
  actual = arr[10];
  expect = 'ten';
  addThis();

  status = inSection(j + cnDASH + 22);
  actual = arr['10'];
  expect = 'ten';
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
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);

  for (var i=0; i<UBound; i++)
  {
    reportCompare(expectedvalues[i], actualvalues[i], statusitems[i]);
  }

  exitFunc ('test');
}
