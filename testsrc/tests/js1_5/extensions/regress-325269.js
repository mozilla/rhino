/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-325269.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 325269;
var summary = 'GC hazard in js_ConstructObject';
var actual = 'No Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
printStatus (summary);
// only get exit code 3 if out of memory error occurs which
// will not happen on machines with enough memory.
// expectExitCode(3);
 
var SavedArray = Array;

function Redirector() { }

Redirector.prototype = 1;
Redirector.__defineGetter__('prototype', function() {
//        printStatus("REDIRECTOR");
			      gc();
			      return SavedArray.prototype;
			    });

//Array = Function('printStatus("Constructor")');
try {
    Array = Function('');
} catch (e) { }

if (Array === SavedArray) {
  // No test of the hazard possible as the array is read-only
  actual = expect;
} else {
  Array.prototype = 1;
  Array.__defineGetter__('prototype', function() {
//        printStatus("**** GETTER ****");
      Array = Redirector;
      gc();
      new Object();
      new Object();
      return undefined;
    });

  new Object();

  try
  {
    var y = "test".split('');
  }
  catch(ex)
  {
    printStatus(ex + '');
  }
}

reportCompare(expect, actual, summary);
