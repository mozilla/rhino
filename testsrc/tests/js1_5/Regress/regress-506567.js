/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-506567.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 506567;
var summary = 'Do not crash with watched variables';
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

  if (typeof clearInterval == 'undefined')
  {
    clearInterval = (function () {});
  }

  var obj = new Object();
  obj.test = null;
  obj.watch("test", (function(prop, oldval, newval)
    {
      if(false)
      {
        var test = newval % oldval;
        var func = (function(){clearInterval(myInterval);});
      }
    }));

  obj.test = 'null';
  print(obj.test);

  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
