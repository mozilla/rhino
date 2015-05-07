/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.3.1.1-3.js';

/**
   File Name:          15.3.1.1-3.js
   ECMA Section:       15.3.1.1 The Function Constructor Called as a Function

   new Function(p1, p2, ..., pn, body )

   Description:        The last argument specifies the body (executable code)
   of a function; any preceeding arguments sepcify formal
   parameters.

   See the text for description of this section.

   This test examples from the specification.

   Author:             christine@netscape.com
   Date:               28 october 1997

*/
var SECTION = "15.3.1.1-3";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "The Function Constructor Called as a Function";

writeHeaderToLog( SECTION + " "+ TITLE);

var args = "";

for ( var i = 0; i < 2000; i++ ) {
  args += "arg"+i;
  if ( i != 1999 ) {
    args += ",";
  }
}

var s = "";

for ( var i = 0; i < 2000; i++ ) {
  s += ".0005";
  if ( i != 1999 ) {
    s += ",";
  }
}

MyFunc = Function( args, "var r=0; for (var i = 0; i < MyFunc.length; i++ ) { if ( eval('arg'+i) == void 0) break; else r += eval('arg'+i); }; return r");
MyObject = Function( args, "for (var i = 0; i < MyFunc.length; i++ ) { if ( eval('arg'+i) == void 0) break; eval('this.arg'+i +'=arg'+i); };");

var MY_OB = eval( "MyFunc("+ s +")" );

new TestCase( SECTION, "MyFunc.length",                       2000,         MyFunc.length );
new TestCase( SECTION, "var MY_OB = eval('MyFunc(s)')",       1,            MY_OB );
new TestCase( SECTION, "var MY_OB = eval('MyFunc(s)')",       1,            eval("var MY_OB = MyFunc("+s+"); MY_OB") );

new TestCase( SECTION, "MyObject.length",                       2000,         MyObject.length );

new TestCase( SECTION, "FUN1 = Function( 'a','b','c', 'return FUN1.length' ); FUN1.length",     3, eval("FUN1 = Function( 'a','b','c', 'return FUN1.length' ); FUN1.length") );
new TestCase( SECTION, "FUN1 = Function( 'a','b','c', 'return FUN1.length' ); FUN1()",          3, eval("FUN1 = Function( 'a','b','c', 'return FUN1.length' ); FUN1()") );
new TestCase( SECTION, "FUN1 = Function( 'a','b','c', 'return FUN1.length' ); FUN1(1,2,3,4,5)", 3, eval("FUN1 = Function( 'a','b','c', 'return FUN1.length' ); FUN1(1,2,3,4,5)") );

test();
