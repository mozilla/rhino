/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-240317.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 240317;
var summary = 'Using Reserved identifiers warns';
var actual = '';
var expect = 'no error';

printBugNumber(BUGNUMBER);
printStatus (summary);

function testvar(words)
{
  var e;
  expect = 'no error';
  for (var i = 0; i < words.length; i++)
  {
    var word = words[i];

    actual = '';
    status = summary + ': ' + word;
    try
    {
      eval('var ' + word + ';');
      actual = 'no error';
    }
    catch(e)
    {
      actual = 'error';
      status +=  ', ' + e.name + ': ' + e.message + ' ';
    }
    reportCompare(expect, actual, status);

    actual = '';
    status = summary + ': ' + word;
    try
    {
      eval(word + ' = "foo";');
      actual = 'no error';
    }
    catch(e)
    {
      actual = 'error';
      status +=  ', ' + e.name + ': ' + e.message + ' ';
    }
    reportCompare(expect, actual, status);

  }
}

// future reserved words
var reserved =
  ['abstract',    'enum',      'int',      'short',      'boolean',
   'interface', 'static',   'byte',       'extends',
   'long',         'super',     'char',     'final',      'native',
   'synchronized', 'class',     'float',    'package',    'throws',
   'goto',      'private',  'transient',             
   'implements',   'protected', 'volatile', 'double',               
   'public'];

testvar(reserved);
 

