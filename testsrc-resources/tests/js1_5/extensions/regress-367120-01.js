/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-367120-01.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 367120;
var summary = 'memory corruption in script_toSource';
var actual = 'No Crash';
var expect = 'No Crash';

//-----------------------------------------------------------------------------
test();
//-----------------------------------------------------------------------------

function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);
 
  if (typeof Script == 'undefined' || !('toSource' in {}))
  {
    print('Test skipped. Script or toSource not defined');
  }
  else
  {
    var s = new Script("");
    var o = {
      valueOf : function() {
        s.compile("");
        Array(11).join(Array(11).join(Array(101).join("aaaaa")));
        return;
      }
    };
    s.toSource(o);
  }
  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
