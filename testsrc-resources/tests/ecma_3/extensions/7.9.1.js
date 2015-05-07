/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = '7.9.1.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 402386;
var summary = 'Automatic Semicolon insertion in restricted statements';
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

  var code;

  code = '(function() { label1: for (;;) { continue \n label1; }})';
  expect = '(function() { label1: for (;;) { continue ; label1; }})';
  actual = uneval(eval(code));
  compareSource(expect, actual, summary + ': ' + code);

  code = '(function() { label2: for (;;) { break \n label2; }})';
  expect = '(function() { label2: for (;;) { break ; label2; }})';
  actual = uneval(eval(code));
  compareSource(expect, actual, summary + ': ' + code);

  code = '(function() { return \n x++; })';
  expect = '(function() { return ; x++; })';
  actual = uneval(eval(code));
  compareSource(expect, actual, summary + ': ' + code);

  print('see bug 256617');
  code = '(function() { throw \n x++; })';
//  expect = '(function() { throw ; x++; })';
  expect = 'SyntaxError: syntax error';
  try { uneval(eval(code)); } catch(ex) { actual = ex + ''; };
//  compareSource(expect, actual, summary + ': ' + code);
  reportCompare(expect, actual, summary + ': ' + code);

  exitFunc ('test');
}

