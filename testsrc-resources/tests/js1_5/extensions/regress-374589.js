/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-374589.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 374589;
var summary = 'Do not assert decompiling try { } catch(x if true) { } ' +
  'catch(y) { } finally { this.a.b; }';
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
 
  var f = function () {
    try { } catch(x if true) { } catch(y) { } finally { this.a.b; } };

  expect = 'function () { try { } catch(x if true) { } catch(y) { } ' +
    'finally { this.a.b; } }';

  actual = f + '';
  compareSource(expect, actual, summary);

  exitFunc ('test');
}
