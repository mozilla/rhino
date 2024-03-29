/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '15.4.2.2-1.js';

/**
   File Name:          15.4.2.2-1.js
   ECMA Section:       15.4.2.2 new Array(len)

   Description:        This description only applies of the constructor is
   given two or more arguments.

   The [[Prototype]] property of the newly constructed
   object is set to the original Array prototype object,
   the one that is the initial value of Array.prototype(0)
   (15.4.3.1).

   The [[Class]] property of the newly constructed object
   is set to "Array".

   If the argument len is a number, then the length
   property  of the newly constructed object is set to
   ToUint32(len).

   If the argument len is not a number, then the length
   property of the newly constructed object is set to 1
   and the 0 property of the newly constructed object is
   set to len.

   This file tests cases where len is a number.

   The cases in this test need to be updated since the
   ToUint32 description has changed.

   Author:             christine@netscape.com
   Date:               7 october 1997
*/
var SECTION = "15.4.2.2-1";
var VERSION = "ECMA_1";
startTest();
var TITLE   = "The Array Constructor:  new Array( len )";

writeHeaderToLog( SECTION + " "+ TITLE);

new TestCase( SECTION,
	      "new Array(0)",            
	      "",                
	      (new Array(0)).toString() );

new TestCase( SECTION,
	      "typeof new Array(0)",     
	      "object",          
	      (typeof new Array(0)) );

new TestCase( SECTION,
	      "(new Array(0)).length",   
	      0,                 
	      (new Array(0)).length );

new TestCase( SECTION,
	      "(new Array(0)).toString",
	      Array.prototype.toString,   
	      (new Array(0)).toString );

new TestCase( SECTION,  
	      "new Array(1)",           
	      "",                
	      (new Array(1)).toString() );

new TestCase( SECTION,  
	      "new Array(1).length",    
	      1,                 
	      (new Array(1)).length );

new TestCase( SECTION,  
	      "(new Array(1)).toString",
	      Array.prototype.toString,  
	      (new Array(1)).toString );

new TestCase( SECTION,
	      "(new Array(-0)).length",                      
	      0, 
	      (new Array(-0)).length );

new TestCase( SECTION,
	      "(new Array(0)).length",                       
	      0, 
	      (new Array(0)).length );

new TestCase( SECTION,
	      "(new Array(10)).length",          
	      10,        
	      (new Array(10)).length );

new TestCase( SECTION,
	      "(new Array('1')).length",         
	      1,         
	      (new Array('1')).length );

new TestCase( SECTION,
	      "(new Array(1000)).length",        
	      1000,      
	      (new Array(1000)).length );

new TestCase( SECTION,
	      "(new Array('1000')).length",      
	      1,         
	      (new Array('1000')).length );

new TestCase( SECTION,
	      "(new Array(4294967295)).length",  
	      ToUint32(4294967295),  
	      (new Array(4294967295)).length );

new TestCase( SECTION,
	      "(new Array('8589934592')).length",
	      1,                     
	      (new Array("8589934592")).length );

new TestCase( SECTION,
	      "(new Array('4294967296')).length",
	      1,                     
	      (new Array("4294967296")).length );

new TestCase( SECTION,
	      "(new Array(1073741824)).length",  
	      ToUint32(1073741824),
	      (new Array(1073741824)).length );

test();

function ToUint32( n ) {
  n = Number( n );
  var sign = ( n < 0 ) ? -1 : 1;

  if ( Math.abs( n ) == 0 || Math.abs( n ) == Number.POSITIVE_INFINITY) {
    return 0;
  }
  n = sign * Math.floor( Math.abs(n) )

    n = n % Math.pow(2,32);

  if ( n < 0 ){
    n += Math.pow(2,32);
  }

  return ( n );
}
