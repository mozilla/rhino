/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'try-008.js';

/**
 *  File Name:          try-008.js
 *  ECMA Section:
 *  Description:        The try statement
 *
 *  This test has a try block in a constructor.
 *
 *
 *  Author:             christine@netscape.com
 *  Date:               11 August 1998
 */
var SECTION = "try-008";
var VERSION = "ECMA_2";
var TITLE   = "The try statement: try in a constructor";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

function Integer( value, exception ) {
  try {
    this.value = checkValue( value );
  } catch ( e ) {
    this.value = e.toString();
  }

  new TestCase(
    SECTION,
    "Integer( " + value +" )",
    (exception ? INVALID_INTEGER_VALUE +": " + value : this.value),
    this.value );
}

var INVALID_INTEGER_VALUE = "Invalid value for java.lang.Integer constructor";

function checkValue( value ) {
  if ( Math.floor(value) != value || isNaN(value) ) {
    throw ( INVALID_INTEGER_VALUE +": " + value );
  } else {
    return value;
  }
}

// add test cases

new Integer( 3, false );
new Integer( NaN, true );
new Integer( 0, false );
new Integer( Infinity, false );
new Integer( -2.12, true );
new Integer( Math.LN2, true );


test();
