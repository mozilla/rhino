/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-355578.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 355578;
var summary = 'block object access to dead JSStackFrame';
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
 
  var filler = "", rooter = {};
  for(var i = 0; i < 0x70/2; i++)
  {
    filler += "\u5050";
  }
  var blkobj = function() { let x; yield function(){}.__parent__; }().next();
  gc();
  for(var i = 0; i < 1024; i++)
  {
    rooter[i] = filler + i;
  }
  try
  {
    print(blkobj.x);
  }
  catch(ex)
  {
  }

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
