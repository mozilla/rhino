/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '11.2.2-4-n.js';

/**
   File Name:          11.2.2-4-n.js
   ECMA Section:       11.2.2. The new operator
   Description:

   MemberExpression:
   PrimaryExpression
   MemberExpression[Expression]
   MemberExpression.Identifier
   new MemberExpression Arguments

   new NewExpression

   The production NewExpression : new NewExpression is evaluated as follows:

   1.   Evaluate NewExpression.
   2.   Call GetValue(Result(1)).
   3.   If Type(Result(2)) is not Object, generate a runtime error.
   4.   If Result(2) does not implement the internal [[Construct]] method,
   generate a runtime error.
   5.   Call the [[Construct]] method on Result(2), providing no arguments
   (that is, an empty list of arguments).
   6.   If Type(Result(5)) is not Object, generate a runtime error.
   7.   Return Result(5).

   The production MemberExpression : new MemberExpression Arguments is evaluated as follows:

   1.   Evaluate MemberExpression.
   2.   Call GetValue(Result(1)).
   3.   Evaluate Arguments, producing an internal list of argument values
   (section 0).
   4.   If Type(Result(2)) is not Object, generate a runtime error.
   5.   If Result(2) does not implement the internal [[Construct]] method,
   generate a runtime error.
   6.   Call the [[Construct]] method on Result(2), providing the list
   Result(3) as the argument values.
   7.   If Type(Result(6)) is not Object, generate a runtime error.
   8    .Return Result(6).

   Author:             christine@netscape.com
   Date:               12 november 1997
*/

var SECTION = "11.2.2-4-n.js";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "The new operator";

writeHeaderToLog( SECTION + " "+ TITLE);

var STRING = "";

DESCRIPTION = "STRING = '', var s = new STRING()";
EXPECTED = "error";

new TestCase( SECTION,
	      "STRING = '', var s = new STRING()",
	      "error",
	      eval("s = new STRING()") );
test();

function TestFunction() {
  return arguments;
}
