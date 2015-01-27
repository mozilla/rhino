/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-340199.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 340199;
var summary = 'User-defined __iterator__ can be called through XPCNativeWrappers';
var actual = 'Not called';
var expect = 'Not called';

printBugNumber(BUGNUMBER);
printStatus (summary);

if (typeof window == 'undefined' ||
    typeof XPCNativeWrapper == 'undefined')
{
  reportCompare("window or XPCNativeWrapper not defined, Test skipped.",
                "window or XPCNativeWrapper not defined, Test skipped.",
                summary);
}
else
{
  Object.prototype.__iterator__ =
    function () { actual = "User code called"; print(actual); };

  try
  {
    for (var i in XPCNativeWrapper(window))
    {
      try
      {
        print(i);
      }
      catch(ex)
      {
        print(ex);
      }
    }
  }
  catch(ex)
  {
  }

  // prevent this from messing up enumerators when shutting down test.
  delete Object.prototype.__iterator__;
}
 
reportCompare(expect, actual, summary);
