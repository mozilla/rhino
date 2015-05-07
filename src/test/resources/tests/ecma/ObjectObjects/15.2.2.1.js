/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.2.2.1.js';

/**
   File Name:          15.2.2.1.js
   ECMA Section:       15.2.2.1 The Object Constructor:  new Object( value )

   1.If the type of the value is not Object, go to step 4.
   2.If the value is a native ECMAScript object, do not create a new object; simply return value.
   3.If the value is a host object, then actions are taken and a result is returned in an
   implementation-dependent manner that may depend on the host object.
   4.If the type of the value is String, return ToObject(value).
   5.If the type of the value is Boolean, return ToObject(value).
   6.If the type of the value is Number, return ToObject(value).
   7.(The type of the value must be Null or Undefined.) Create a new native ECMAScript object.
   The [[Prototype]] property of the newly constructed object is set to the Object prototype object.
   The [[Class]] property of the newly constructed object is set to "Object".
   The newly constructed object has no [[Value]] property.
   Return the newly created native object.

   Description:        This does not test cases where the object is a host object.
   Author:             christine@netscape.com
   Date:               7 october 1997
*/

var SECTION = "15.2.2.1";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "new Object( value )";

writeHeaderToLog( SECTION + " "+ TITLE);


new TestCase( SECTION,  "typeof new Object(null)",      "object",           typeof new Object(null) );
new TestCase( SECTION,  "MYOB = new Object(null); MYOB.toString = Object.prototype.toString; MYOB.toString()",  "[object Object]",   eval("MYOB = new Object(null); MYOB.toString = Object.prototype.toString; MYOB.toString()") );

new TestCase( SECTION,  "typeof new Object(void 0)",      "object",           typeof new Object(void 0) );
new TestCase( SECTION,  "MYOB = new Object(new Object(void 0)); MYOB.toString = Object.prototype.toString; MYOB.toString()",  "[object Object]",   eval("MYOB = new Object(new Object(void 0)); MYOB.toString = Object.prototype.toString; MYOB.toString()") );

new TestCase( SECTION,  "typeof new Object('string')",      "object",           typeof new Object('string') );
new TestCase( SECTION,  "MYOB = (new Object('string'); MYOB.toString = Object.prototype.toString; MYOB.toString()",  "[object String]",   eval("MYOB = new Object('string'); MYOB.toString = Object.prototype.toString; MYOB.toString()") );
new TestCase( SECTION,  "(new Object('string').valueOf()",  "string",           (new Object('string')).valueOf() );

new TestCase( SECTION,  "typeof new Object('')",            "object",           typeof new Object('') );
new TestCase( SECTION,  "MYOB = (new Object(''); MYOB.toString = Object.prototype.toString; MYOB.toString()",  "[object String]",   eval("MYOB = new Object(''); MYOB.toString = Object.prototype.toString; MYOB.toString()") );
new TestCase( SECTION,  "(new Object('').valueOf()",        "",                 (new Object('')).valueOf() );

new TestCase( SECTION,  "typeof new Object(Number.NaN)",      "object",                 typeof new Object(Number.NaN) );
new TestCase( SECTION,  "MYOB = (new Object(Number.NaN); MYOB.toString = Object.prototype.toString; MYOB.toString()",  "[object Number]",   eval("MYOB = new Object(Number.NaN); MYOB.toString = Object.prototype.toString; MYOB.toString()") );
new TestCase( SECTION,  "(new Object(Number.NaN).valueOf()",  Number.NaN,               (new Object(Number.NaN)).valueOf() );

new TestCase( SECTION,  "typeof new Object(0)",      "object",                 typeof new Object(0) );
new TestCase( SECTION,  "MYOB = (new Object(0); MYOB.toString = Object.prototype.toString; MYOB.toString()",  "[object Number]",   eval("MYOB = new Object(0); MYOB.toString = Object.prototype.toString; MYOB.toString()") );
new TestCase( SECTION,  "(new Object(0).valueOf()",  0,               (new Object(0)).valueOf() );

new TestCase( SECTION,  "typeof new Object(-0)",      "object",                 typeof new Object(-0) );
new TestCase( SECTION,  "MYOB = (new Object(-0); MYOB.toString = Object.prototype.toString; MYOB.toString()",  "[object Number]",   eval("MYOB = new Object(-0); MYOB.toString = Object.prototype.toString; MYOB.toString()") );
new TestCase( SECTION,  "(new Object(-0).valueOf()",  -0,               (new Object(-0)).valueOf() );

new TestCase( SECTION,  "typeof new Object(1)",      "object",                 typeof new Object(1) );
new TestCase( SECTION,  "MYOB = (new Object(1); MYOB.toString = Object.prototype.toString; MYOB.toString()",  "[object Number]",   eval("MYOB = new Object(1); MYOB.toString = Object.prototype.toString; MYOB.toString()") );
new TestCase( SECTION,  "(new Object(1).valueOf()",  1,               (new Object(1)).valueOf() );

new TestCase( SECTION,  "typeof new Object(-1)",      "object",                 typeof new Object(-1) );
new TestCase( SECTION,  "MYOB = (new Object(-1); MYOB.toString = Object.prototype.toString; MYOB.toString()",  "[object Number]",   eval("MYOB = new Object(-1); MYOB.toString = Object.prototype.toString; MYOB.toString()") );
new TestCase( SECTION,  "(new Object(-1).valueOf()",  -1,               (new Object(-1)).valueOf() );

new TestCase( SECTION,  "typeof new Object(true)",      "object",                 typeof new Object(true) );
new TestCase( SECTION,  "MYOB = (new Object(true); MYOB.toString = Object.prototype.toString; MYOB.toString()",  "[object Boolean]",   eval("MYOB = new Object(true); MYOB.toString = Object.prototype.toString; MYOB.toString()") );
new TestCase( SECTION,  "(new Object(true).valueOf()",  true,               (new Object(true)).valueOf() );

new TestCase( SECTION,  "typeof new Object(false)",      "object",              typeof new Object(false) );
new TestCase( SECTION,  "MYOB = (new Object(false); MYOB.toString = Object.prototype.toString; MYOB.toString()",  "[object Boolean]",   eval("MYOB = new Object(false); MYOB.toString = Object.prototype.toString; MYOB.toString()") );
new TestCase( SECTION,  "(new Object(false).valueOf()",  false,                 (new Object(false)).valueOf() );

new TestCase( SECTION,  "typeof new Object(Boolean())",         "object",               typeof new Object(Boolean()) );
new TestCase( SECTION,  "MYOB = (new Object(Boolean()); MYOB.toString = Object.prototype.toString; MYOB.toString()",  "[object Boolean]",   eval("MYOB = new Object(Boolean()); MYOB.toString = Object.prototype.toString; MYOB.toString()") );
new TestCase( SECTION,  "(new Object(Boolean()).valueOf()",     Boolean(),              (new Object(Boolean())).valueOf() );


var myglobal    = this;
var myobject    = new Object( "my new object" );
var myarray     = new Array();
var myboolean   = new Boolean();
var mynumber    = new Number();
var mystring    = new String();
var myobject    = new Object();
var myfunction  = new Function( "x", "return x");
var mymath      = Math;

new TestCase( SECTION, "myglobal = new Object( this )",                     myglobal,       new Object(this) );
new TestCase( SECTION, "myobject = new Object('my new object'); new Object(myobject)",            myobject,       new Object(myobject) );
new TestCase( SECTION, "myarray = new Array(); new Object(myarray)",        myarray,        new Object(myarray) );
new TestCase( SECTION, "myboolean = new Boolean(); new Object(myboolean)",  myboolean,      new Object(myboolean) );
new TestCase( SECTION, "mynumber = new Number(); new Object(mynumber)",     mynumber,       new Object(mynumber) );
new TestCase( SECTION, "mystring = new String9); new Object(mystring)",     mystring,       new Object(mystring) );
new TestCase( SECTION, "myobject = new Object(); new Object(mynobject)",    myobject,       new Object(myobject) );
new TestCase( SECTION, "myfunction = new Function(); new Object(myfunction)", myfunction,   new Object(myfunction) );
new TestCase( SECTION, "mymath = Math; new Object(mymath)",                 mymath,         new Object(mymath) );

test();
