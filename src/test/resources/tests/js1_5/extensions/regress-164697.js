/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var gTestfile = 'regress-164697.js';
//-----------------------------------------------------------------------------
var BUGNUMBER = 164697;
var summary = '(instance.__parent__ == constructor.__parent__)';
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
printStatus (summary);

expect = 'true';

runtest('{}', 'Object');
runtest('new Object()', 'Object');

// see https://bugzilla.mozilla.org/show_bug.cgi?id=321669
// for why this test is not contained in a function.
actual = (function (){}).__proto__ == Function.prototype;
reportCompare('true', actual+'',
              '(function (){}).__proto__ == Function.prototype');

runtest('new Function(";")', 'Function');

runtest('[]', 'Array');
runtest('new Array()', 'Array');

runtest('""', 'String');
runtest('new String()', 'String');

runtest('true', 'Boolean');
runtest('new Boolean()', 'Boolean');

runtest('1', 'Number');
runtest('new Number("1")', 'Number');

runtest('new Date()', 'Date');

runtest('/x/', 'RegExp');
runtest('new RegExp("x")', 'RegExp');

runtest('new Error()', 'Error');

function runtest(myinstance, myconstructor)
{
  var expr;
  var actual;

  try
  {
    expr =  '(' + myinstance + ').__parent__ == ' +
      myconstructor + '.__parent__';
    printStatus(expr);
    actual = eval(expr).toString();
  }
  catch(ex)
  {
    actual = ex + '';
  }

  reportCompare(expect, actual, expr);

  try
  {
    expr =  '(' + myinstance + ').__proto__ == ' +
      myconstructor + '.prototype';
    printStatus(expr);
    actual = eval(expr).toString();
  }
  catch(ex)
  {
    actual = ex + '';
  }

  reportCompare(expect, actual, expr);
}
