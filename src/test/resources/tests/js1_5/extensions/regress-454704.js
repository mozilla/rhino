/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-454704.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 454704;
var summary = 'Do not crash with defineGetter and XPC wrapper';
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

  if (typeof XPCSafeJSObjectWrapper != 'undefined' && typeof document != 'undefined')
  {
    gDelayTestDriverEnd = true;
    document.addEventListener('load', boom, true);
  }
  else
  {
    print(expect = actual = 'Test requires browser.');
    reportCompare(expect, actual, summary);
  }
  exitFunc ('test');
}

function boom()
{
  try
  {
    var a = [];
    g = [];
    g.__defineGetter__("f", g.toSource);
    a[0] = g;
    a[1] = XPCSafeJSObjectWrapper(a);
    print("" + a);
  }
  catch(ex)
  {
    print(ex + '');
  }
  gDelayTestDriverEnd = false;
  jsTestDriverEnd();
  reportCompare(expect, actual, summary);
}

