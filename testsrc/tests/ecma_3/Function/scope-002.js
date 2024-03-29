/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
 * Date: 28 May 2001
 *
 * SUMMARY:  Functions are scoped statically, not dynamically
 *
 * See ECMA Section 10.1.4 Scope Chain and Identifier Resolution
 * (This section defines the scope chain of an execution context)
 *
 * See ECMA Section 12.10 The with Statement
 *
 * See ECMA Section 13 Function Definition
 * (This section defines the scope chain of a function object as that
 *  of the running execution context when the function was declared)
 *
 * Like scope-001.js, but using assignment var f = function expression
 * instead of a function declaration: function f() {} etc.
 */
//-----------------------------------------------------------------------------
var gTestfile = 'scope-002.js';
var UBound = 0;
var BUGNUMBER = '(none)';
var summary = 'Testing that functions are scoped statically, not dynamically';
var self = this;  // capture a reference to the global object
var status = '';
var statusitems = [ ];
var actual = '';
var actualvalues = [ ];
var expect= '';
var expectedvalues = [ ];


/*
 * In this section the expected value is 1, not 2.
 *
 * Why? f captures its scope chain from when it's declared, and imposes that chain
 * when it's executed. In other words, f's scope chain is from when it was compiled.
 * Since f is a top-level function, this is the global object only. Hence 'a' resolves to 1.
 */
status = 'Section A of test';
var a = 1;
var f = function () {return a;};
var obj = {a:2};
with (obj)
{
  actual = f();
}
expect = 1;
addThis();


/*
 * In this section the expected value is 2, not 1. That is because here
 * f's associated scope chain now includes 'obj' before the global object.
 */
status = 'Section B of test';
var a = 1;
var obj = {a:2};
with (obj)
{
  var f = function () {return a;};
  actual = f();
}
expect = 2;
addThis();


/*
 * Like Section B , except that we call f outside the with block.
 * By the principles explained above, we still expect 2 -
 */
status = 'Section C of test';
var a = 1;
var obj = {a:2};
with (obj)
{
  var f = function () {return a;};
}
actual = f();
expect = 2;
addThis();


/*
 * Like Section C, but with one more level of indirection -
 */
status = 'Section D of test';
var a = 1;
var obj = {a:2, obj:{a:3}};
with (obj)
{
  with (obj)
  {
    var f = function () {return a;};
  }
}
actual = f();
expect = 3;
addThis();


/*
 * Like Section C, but here we actually delete obj before calling f.
 * We still expect 2 -
 */
status = 'Section E of test';
var a = 1;
var obj = {a:2};
with (obj)
{
  var f = function () {return a;};
}
delete obj;
actual = f();
expect = 2;
addThis();


/*
 * Like Section E. Here we redefine obj and call f under with (obj) -
 * We still expect 2 -
 */
status = 'Section F of test';
var a = 1;
var obj = {a:2};
with (obj)
{
  var f = function () {return a;};
}
delete obj;
var obj = {a:3};
with (obj)
{
  actual = f();
}
expect = 2;  // NOT 3 !!!
addThis();


/*
 * Explicitly verify that f exists at global level, even though
 * it was defined under the with(obj) block -
 */
status = 'Section G of test';
var a = 1;
var obj = {a:2};
with (obj)
{
  var f = function () {return a;};
}
actual = String([obj.hasOwnProperty('f'), self.hasOwnProperty('f')]);
expect = String([false, true]);
addThis();


/*
 * Explicitly verify that f exists at global level, even though
 * it was defined under the with(obj) block -
 */
status = 'Section H of test';
var a = 1;
var obj = {a:2};
with (obj)
{
  var f = function () {return a;};
}
actual = String(['f' in obj, 'f' in self]);
expect = String([false, true]);
addThis();



//-------------------------------------------------------------------------------------------------
test();
//-------------------------------------------------------------------------------------------------


function addThis()
{
  statusitems[UBound] = status;
  actualvalues[UBound] = actual;
  expectedvalues[UBound] = expect;
  UBound++;
  resetTestVars();
}


function resetTestVars()
{
  delete a;
  delete obj;
  delete f;
}


function test()
{
  enterFunc ('test');
  printBugNumber(BUGNUMBER);
  printStatus (summary);

  for (var i = 0; i < UBound; i++)
  {
    reportCompare(expectedvalues[i], actualvalues[i], statusitems[i]);
  }

  exitFunc ('test');
}
