/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 *
 * Date:    07 Oct 2002
 * SUMMARY: UTF-8 decoder should not accept overlong sequences
 * See http://bugzilla.mozilla.org/show_bug.cgi?id=172699
 *
 */
//-----------------------------------------------------------------------------
var gTestfile = 'regress-172699.js';
var UBound = 0;
var BUGNUMBER = 172699;
var summary = 'UTF-8 decoder should not accept overlong sequences';
var status = '';
var statusitems = [];
var actual = '';
var actualvalues = [];
var expect= '';
var expectedvalues = [];

/*
 * The patch for http://bugzilla.mozilla.org/show_bug.cgi?id=172699
 * defined this value to be the result of an overlong UTF-8 sequence -
 */
var INVALID_CHAR = 0xFFFD;


status = inSection(1);
actual = decodeURI("%C0%AF").charCodeAt(0);
expect = INVALID_CHAR;
addThis();



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
