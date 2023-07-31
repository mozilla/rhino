/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.netscape.javascript.qa.lc3.number;

/**
 *  3.4 Preferred Argument Conversions
 *
 */

public class Number_001 {
    public String BOOLEAN = "BOOLEAN";
    public String BOOLEAN_OBJECT = "BOOLEAN_OBJECT";
    public String DOUBLE_OBJECT =  "DOUBLE_OBJECT";
    public String OBJECT = "OBJECT";
    public String STRING = "STRING";
    public String LONG   = "LONG";
    public String INT    = "INT";
    public String SHORT  = "SHORT";
    public String CHAR   = "CHAR";
    public String BYTE   = "BYTE";
    public String DOUBLE = "DOUBLE";
    public String FLOAT  = "FLOAT";

    public String ambiguous( Object arg ) {
        return OBJECT;
    }

    public String ambiguous( String arg ) {
        return STRING;
    }

    public String ambiguous( byte arg ) {
        return BYTE;
    }

    public String ambiguous( char arg ) {
        return CHAR;
    }

    public String ambiguous( short arg ) {
        return SHORT;
    }

    public String ambiguous( int arg ) {
        return INT;
    }

    public String ambiguous( long arg ) {
        return LONG;
    }

    public String ambiguous( float arg ) {
        return FLOAT;
    }

    public String ambiguous( Double arg ) {
        return DOUBLE_OBJECT;
    }

    public String ambiguous( double arg ) {
        return DOUBLE;
    }

    public String expect() {
        return DOUBLE;
    }

}