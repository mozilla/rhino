/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.3.5-1.js';

/**
   File Name:          15.3.5-1.js
   ECMA Section:       15.3.5 Properties of Function Instances
   new Function(p1, p2, ..., pn, body )

   Description:

   15.3.5.1 length

   The value of the length property is usually an integer that indicates
   the "typical" number of arguments expected by the function. However,
   the language permits the function to be invoked with some other number
   of arguments. The behavior of a function when invoked on a number of
   arguments other than the number specified by its length property depends
   on the function.

   15.3.5.2 prototype
   The value of the prototype property is used to initialize the internal [[
   Prototype]] property of a newly created object before the Function object
   is invoked as a constructor for that newly created object.

   15.3.5.3 arguments

   The value of the arguments property is normally null if there is no
   outstanding invocation of the function in progress (that is, the function has been called
   but has not yet returned). When a non-internal Function object (15.3.2.1) is invoked, its
   arguments property is "dynamically bound" to a newly created object that contains the
   arguments on which it was invoked (see 10.1.6 and 10.1.8). Note that the use of this
   property is discouraged; it is provided principally for compatibility with existing old code.

   Author:             christine@netscape.com
   Date:               28 october 1997

*/

var SECTION = "15.3.5-1";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "Properties of Function Instances";

writeHeaderToLog( SECTION + " "+TITLE);

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

MyFunc = new Function( args, "var r=0; for (var i = 0; i < MyFunc.length; i++ ) { if ( eval('arg'+i) == void 0) break; else r += eval('arg'+i); }; return r");
MyObject = new Function( args, "for (var i = 0; i < MyFunc.length; i++ ) { if ( eval('arg'+i) == void 0) break; eval('this.arg'+i +'=arg'+i); };");


new TestCase( SECTION, "MyFunc.length",                       2000,         MyFunc.length );
new TestCase( SECTION, "var MY_OB = eval('MyFunc(s)')",       1,            eval("var MY_OB = MyFunc("+s+"); MY_OB") );
new TestCase( SECTION, "MyFunc.prototype.toString()",       "[object Object]",  MyFunc.prototype.toString() );
new TestCase( SECTION, "typeof MyFunc.prototype",           "object",           typeof MyFunc.prototype );


new TestCase( SECTION, "MyObject.length",                       2000,         MyObject.length );

new TestCase( SECTION, "FUN1 = new Function( 'a','b','c', 'return FUN1.length' ); FUN1.length",     3, eval("FUN1 = new Function( 'a','b','c', 'return FUN1.length' ); FUN1.length") );
new TestCase( SECTION, "FUN1 = new Function( 'a','b','c', 'return FUN1.length' ); FUN1()",          3, eval("FUN1 = new Function( 'a','b','c', 'return FUN1.length' ); FUN1()") );
new TestCase( SECTION, "FUN1 = new Function( 'a','b','c', 'return FUN1.length' ); FUN1(1,2,3,4,5)", 3, eval("FUN1 = new Function( 'a','b','c', 'return FUN1.length' ); FUN1(1,2,3,4,5)") );

test();
