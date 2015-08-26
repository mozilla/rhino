/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-414098.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 414098;
var summary = 'Getter behavior on arrays';
var actual = '';
var expect = '';

var a=[1,2,3];
var foo = 44;
a.__defineGetter__(1, function() { return foo + 10; });
actual = String(a);
reportCompare("1,54,3", actual, "getter 1");

actual = String(a.reverse());
reportCompare("3,54,1", actual, "reverse");

var s = "";
a.forEach(function(e) { s += e + "|"; });
actual = s;
reportCompare("3|54|1|", actual, "forEach");

actual = a.join(' - ');
reportCompare("3 - 54 - 1", actual, "join");

try
{
  actual = String(a.sort());
}
catch(ex)
{
  actual = ex + '';
}
reportCompare("TypeError: setting a property that has only a getter", actual, "sort");

try
{
  a[2]=3;
}
catch(ex)
{
  actual = ex + '';
}
reportCompare("TypeError: setting a property that has only a getter", actual, "setter");

actual = a.pop();
reportCompare(3, actual, "pop");

actual = a.pop();
reportCompare(54, actual, "pop 2");
