/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'yflag.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 371932;
var summary = 'ES4 Regular Expression /y flag';
var actual = '';
var expect = '';

print('See http://developer.mozilla.org/es4/proposals/extend_regexps.html#y_flag');

//-----------------------------------------------------------------------------
test();
//-----------------------------------------------------------------------------

function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);

  var c;
  var s = '123456';

  print('Test global flag.');

  var g = /(1)/g;
  expect = 'captures: 1,1; RegExp.leftContext: ""; RegExp.rightContext: "234561"';
  actual = 'captures: ' + g.exec('1234561') +
    '; RegExp.leftContext: "' + RegExp.leftContext +
    '"; RegExp.rightContext: "' + RegExp.rightContext + '"';
  reportCompare(expect, actual, summary + ' - /(1)/g.exec("1234561") first call');

  expect = 'captures: 1,1; RegExp.leftContext: "123456"; RegExp.rightContext: ""';
  actual = 'captures: ' + g.exec('1234561') +
    '; RegExp.leftContext: "' + RegExp.leftContext +
    '"; RegExp.rightContext: "' + RegExp.rightContext + '"';
  reportCompare(expect, actual, summary + ' - /(1)/g.exec("1234561") second call');
  var y = /(1)/y;
 
  print('Test sticky flag.');

  var y = /(1)/y;
  expect = 'captures: 1,1; RegExp.leftContext: ""; RegExp.rightContext: "234561"';
  actual = 'captures: ' + y.exec('1234561') +
    '; RegExp.leftContext: "' + RegExp.leftContext +
    '"; RegExp.rightContext: "' + RegExp.rightContext + '"';
  reportCompare(expect, actual, summary + ' - /(1)/y.exec("1234561") first call');

  expect = 'captures: null; RegExp.leftContext: ""; RegExp.rightContext: "234561"';
  actual = 'captures: ' + y.exec('1234561') +
    '; RegExp.leftContext: "' + RegExp.leftContext +
    '"; RegExp.rightContext: "' + RegExp.rightContext + '"';
  reportCompare(expect, actual, summary + ' - /(1)/y.exec("1234561") second call');
  var y = /(1)/y;
 
  reportCompare(expect, actual, summary);

  y = /(1)/y;
  expect = 'captures: 1,1; RegExp.leftContext: ""; RegExp.rightContext: "123456"';
  actual = 'captures: ' + y.exec('1123456') +
    '; RegExp.leftContext: "' + RegExp.leftContext +
    '"; RegExp.rightContext: "' + RegExp.rightContext + '"';
  reportCompare(expect, actual, summary + ' - /(1)/y.exec("1123456") first call');

  expect = 'captures: 1,1; RegExp.leftContext: "1"; RegExp.rightContext: "23456"';
  actual = 'captures: ' + y.exec('1123456') +
    '; RegExp.leftContext: "' + RegExp.leftContext +
    '"; RegExp.rightContext: "' + RegExp.rightContext + '"';
  reportCompare(expect, actual, summary + ' - /(1)/y.exec("1123456") second call');
  var y = /(1)/y;
 
  reportCompare(expect, actual, summary);

  exitFunc ('test');
}
