/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-352060.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 352060;
var summary = 'decompilation of getter, setter revisited';
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
 
  var f;

  f = function() { foo setter = function(){} }
  expect = 'function() { foo setter = function(){}; }';
  actual = f + '';
  compareSource(expect, actual, summary);

  f = function() { foo.bar setter = function(){} }
  expect = 'function() { foo.bar setter = function(){}; }';
  actual = f + '';
  compareSource(expect, actual, summary);

  f = function(){ var y = new Array(); y[0] getter = function(){}; }
  expect = 'function(){ var y = new Array(); y[0] getter = function(){}; } ';
  actual = f + '';
  compareSource(expect, actual, summary);

  f = function(){ var foo = <foo bar="baz"/>; foo.@bar getter = function(){}; }
  expect = 'function(){ var foo = <foo bar="baz"/>; foo.@bar getter = function(){}; }';
  actual = f + '';
  compareSource(expect, actual, summary);

  exitFunc ('test');
}
