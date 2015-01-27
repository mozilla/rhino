/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-312278.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 312278;
var summary = 'Do no access GC-ed object in Error.prototype.toSource';
var actual = 'No Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
printStatus (summary);
 
function wrapInsideWith(obj)
{
  var f;
  with (obj) {
    f = function() { }
  }
  return f.__parent__;
}

function customToSource()
{
  return "customToSource "+this;
}

Error.prototype.__defineGetter__('message', function() {
				   var obj = {
				     toSource: "something"
				   }
				   obj.__defineGetter__('toSource', function() {
							  gc();
							  return customToSource;
							});
				   return wrapInsideWith(obj);
				 });

printStatus(Error.prototype.toSource());

reportCompare(expect, actual, summary);
