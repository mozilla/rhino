/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-341360.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 341360;
var summary = 'clearInterval broken';
var actual = '';
var expect = 'Ok';

printBugNumber(BUGNUMBER);
printStatus (summary);

function xxx()
{
  if(t != null)
  {
    print('Clearing interval...');
    window.clearInterval(t);
    t = null;
    setTimeout('yyy()', 2000);
   
  }
  else {
    print('Clearing interval failed...');
    actual = "Broken";
    gDelayTestDriverEnd = false;
    reportCompare(expect, actual, summary);
    jsTestDriverEnd();
  }
}

function yyy()
{
  print('Checking result...');
  actual = 'Ok';
  gDelayTestDriverEnd = false;
  reportCompare(expect, actual, summary);
  jsTestDriverEnd();
}

if (typeof window == 'undefined')
{
  expect = actual = 'Not tested';
  reportCompare(expect, actual, summary);
}
else
{
  print('Start...');
  gDelayTestDriverEnd = true;
  var t = window.setInterval(xxx, 1000);
}

