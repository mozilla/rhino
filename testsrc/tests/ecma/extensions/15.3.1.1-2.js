/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.3.1.1-2.js';

/**
   File Name:          15.3.1.1-2.js
   ECMA Section:       15.3.1.1 The Function Constructor Called as a Function
   Function(p1, p2, ..., pn, body )

   Description:
   When the Function function is called with some arguments p1, p2, . . . , pn,
   body (where n might be 0, that is, there are no "p" arguments, and where body
   might also not be provided), the following steps are taken:

   1.  Create and return a new Function object exactly if the function constructor
   had been called with the same arguments (15.3.2.1).

   Author:             christine@netscape.com
   Date:               28 october 1997

*/
var SECTION = "15.3.1.1-2";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "The Function Constructor Called as a Function";

writeHeaderToLog( SECTION + " "+ TITLE);

var myfunc2 =  Function("a, b, c",   "return a+b+c" );
var myfunc3 =  Function("a,b", "c",  "return a+b+c" );

myfunc2.toString = Object.prototype.toString;
myfunc3.toString = Object.prototype.toString;


new TestCase( SECTION, 
	      "myfunc2.__proto__",                        
	      Function.prototype,    
	      myfunc2.__proto__ );

new TestCase( SECTION, 
	      "myfunc3.__proto__",                        
	      Function.prototype,    
	      myfunc3.__proto__ );

test();
