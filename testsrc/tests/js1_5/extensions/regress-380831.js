/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-380831.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 380831;
var summary = 'uneval trying to output a getter function that is a sharp definition';
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

  expect = '( { b getter : # 1 = ( function ( ) { } ) , c getter : # 1 # } )';
  a = {}; 
  f = function() { }; 
  a.b getter = f; 
  a.c getter = f;
  actual = uneval(a);
  compareSource(expect, actual, summary);

  expect = 'function ( ) { return { get x ( ) { } } ; }';
  f = function() { return { x getter: function(){} } };
  actual = f + '';
  compareSource(expect, actual, summary);

  expect = 'function ( ) { return { x getter : # 1 = function ( ) { } } ; }';
  f = function() { return { x getter: #1=function(){} } }; 
  actual = f + '';
  compareSource(expect, actual, summary);

  expect = 'function ( ) { return { x getter : # 1 # } ; }';
  f = function() { return { x getter: #1# } };
  actual = f + '';
  compareSource(expect, actual, summary);

  exitFunc ('test');
}
