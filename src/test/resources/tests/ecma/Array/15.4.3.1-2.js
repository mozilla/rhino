/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.4.3.1-2.js';

/**
   File Name:          15.4.3.1-1.js
   ECMA Section:       15.4.3.1 Array.prototype
   Description:        The initial value of Array.prototype is the built-in
   Array prototype object (15.4.4).

   Author:             christine@netscape.com
   Date:               7 october 1997
*/

var SECTION = "15.4.3.1-1";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "Array.prototype";

writeHeaderToLog( SECTION + " "+ TITLE);


var ARRAY_PROTO = Array.prototype;

new TestCase( SECTION, 
	      "var props = ''; for ( p in Array  ) { props += p } props",
	      "",
	      eval("var props = ''; for ( p in Array  ) { props += p } props") );

new TestCase( SECTION, 
	      "Array.prototype = null; Array.prototype",  
	      ARRAY_PROTO,
	      eval("Array.prototype = null; Array.prototype") );

new TestCase( SECTION, 
	      "delete Array.prototype",                  
	      false,      
	      delete Array.prototype );

new TestCase( SECTION, 
	      "delete Array.prototype; Array.prototype", 
	      ARRAY_PROTO,
	      eval("delete Array.prototype; Array.prototype") );

test();
