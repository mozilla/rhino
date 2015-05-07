/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'constructor.js';

/**
 * Tests constructing some Java classes from JS.
 * Tests constructor matching algorithm.
 *
 * this needs to be converted to the new test structure
 *
 * @author cbegle and mikeang
 */

var SECTION = "wrapUnwrap.js";
var VERSION = "JS1_3";
var TITLE   = "LiveConnect";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

var char_object = java.lang.Character.forDigit(22, 36);
test_typeof( "string", char_object+"a" );

var boolean_object = new java.lang.Boolean( true );
test_class( "java.lang.Boolean", boolean_object );

var boolean_object = new java.lang.Boolean( false );
test_class( "java.lang.Boolean", boolean_object );

var integer_object = new java.lang.Integer( 12345 );
test_class( "java.lang.Integer", integer_object );

var string_object = new java.lang.String( "string object value" );
test_class( "java.lang.String", string_object );

// This doesn't work - bug #105857
var float_object = new java.lang.Float( .009 * .009 );
test_class( "java.lang.Float", float_object );

var double_object = new java.lang.Double( java.lang.Math.PI );
test_class( "java.lang.Double", double_object );

var long_object = new java.lang.Long( 92233720368547760 );
test_class( "java.lang.Long", long_object );

var rect_object = new java.awt.Rectangle( 0, 0, 100, 100 );
test_class ( "java.awt.Rectangle", rect_object );

test();


function test_typeof( eType, someObject ) {
  new TestCase( SECTION,
		"typeof( " +someObject+")",
		eType,
		typeof someObject );
}

/**
 * Implements Java instanceof keyword.
 */
function test_class( eClass, javaObject ) {
  new TestCase( SECTION,
		javaObject +".getClass().equals( java.lang.Class.forName( " +
		eClass +")",
		true,
		(javaObject.getClass()).equals( java.lang.Class.forName(eClass)) );
}
