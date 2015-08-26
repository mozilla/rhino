/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-372309.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 372309;
var summary = 'Root new array objects';
var actual = 'No Crash';
var expect = 'No Crash';

function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);

  var width = 600;
  var height = 600;

  var img1canvas = document.createElement("canvas");
  var img2canvas = document.createElement("canvas");

  img1canvas.width = img2canvas.width = width;
  img1canvas.height = img2canvas.height = height;
  img1canvas.getContext("2d").getImageData(0, 0, width, height).data;
  img2canvas.getContext("2d").getImageData(0, 0, width, height).data;
 
  reportCompare(expect, actual, summary);
  gDelayTestDriverEnd = false;
  jsTestDriverEnd();

  exitFunc ('test');
}

if (typeof window != 'undefined')
{
  // delay test driver end
  gDelayTestDriverEnd = true;

  window.addEventListener("load", test, false);
}
else
{
  reportCompare(expect, actual, summary);
}

