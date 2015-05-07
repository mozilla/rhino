/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '10.1.8-2.js';

/**
   File Name:          10.1.8-2
   ECMA Section:       Arguments Object
   Description:

   When control enters an execution context for declared function code,
   anonymous code, or implementation-supplied code, an arguments object is
   created and initialized as follows:

   The [[Prototype]] of the arguments object is to the original Object
   prototype object, the one that is the initial value of Object.prototype
   (section 15.2.3.1).

   A property is created with name callee and property attributes {DontEnum}.
   The initial value of this property is the function object being executed.
   This allows anonymous functions to be recursive.

   A property is created with name length and property attributes {DontEnum}.
   The initial value of this property is the number of actual parameter values
   supplied by the caller.

   For each non-negative integer, iarg, less than the value of the length
   property, a property is created with name ToString(iarg) and property
   attributes { DontEnum }. The initial value of this property is the value
   of the corresponding actual parameter supplied by the caller. The first
   actual parameter value corresponds to iarg = 0, the second to iarg = 1 and
   so on. In the case when iarg is less than the number of formal parameters
   for the function object, this property shares its value with the
   corresponding property of the activation object. This means that changing
   this property changes the corresponding property of the activation object
   and vice versa. The value sharing mechanism depends on the implementation.

   Author:             christine@netscape.com
   Date:               12 november 1997
*/

var SECTION = "10.1.8-2";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "Arguments Object";

writeHeaderToLog( SECTION + " "+ TITLE);

//  Tests for anonymous functions

var GetCallee       = new Function( "var c = arguments.callee; return c" );
var GetArguments    = new Function( "var a = arguments; return a" );
var GetLength       = new Function( "var l = arguments.length; return l" );

var ARG_STRING = "value of the argument property";

new TestCase( SECTION,
	      "GetCallee()",
	      GetCallee,
	      GetCallee() );

var LIMIT = 100;

for ( var i = 0, args = "" ; i < LIMIT; i++ ) {
  args += String(i) + ( i+1 < LIMIT ? "," : "" );

}

var LENGTH = eval( "GetLength("+ args +")" );

new TestCase( SECTION,
	      "GetLength("+args+")",
	      100,
	      LENGTH );

var ARGUMENTS = eval( "GetArguments( " +args+")" );

for ( var i = 0; i < 100; i++ ) {
  new TestCase( SECTION,
		"GetArguments("+args+")["+i+"]",
		i,
		ARGUMENTS[i] );
}

test();
