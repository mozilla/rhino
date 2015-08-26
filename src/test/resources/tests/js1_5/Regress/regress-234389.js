/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-234389.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 234389;
var summary = 'Do not Crash when overloaded toString causes infinite recursion';
var actual = ''
  var expect = 'Internal Error: too much recursion';

printBugNumber(BUGNUMBER);
printStatus (summary);

var foo = {
  toString: function() {
    if (this.re.test(this)) {
      return "";
    }
    return this.value;
  },
 
  value: "foo",
 
  re: /bar/
};

try
{
  var f = foo.toString();
  expect = 'No Crash';
  actual = 'No Crash';
}
catch(ex)
{
  expect = 'InternalError: too much recursion';
  actual = ex.name + ': ' + ex.message;
}
reportCompare(expect, actual, summary);
