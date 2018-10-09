/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */


package com.netscape.javascript.qa.liveconnect.datatypes;

import com.netscape.javascript.qa.liveconnect.*;

/**
 *  Tests for converting Java short[] to JavaScript variables.  
 *
 *  <p>
 *
 *  Create a JavaScript variable.  Use JSObject.eval to call a Java method
 *  that returns a Java array.  Assign the array to the JavaScript variable.
 *
 *  <p>
 *
 *  Iterate through the array in JavaScript.  Get the value, and compare it
 *  to the value in the original Java array.
 *
 *  <p>
 *
 *  Use JSObject.getMember to get the JavaScript variable.  Verify that the
 *  array is the same object that was used to assign the original JS variable.
 *
 *  @see com.netscape.javascript.qa.liveconnect.DataTypesClass
 *  @see netscape.javascript.JSObject
 *
 *  @author christine m begle
 */

public class DataTypes_011 extends LiveConnectTest {
    public DataTypes_011() {
        super();
    }

    public static void main( String[] args ) {
        DataTypes_011 test = new DataTypes_011();
        test.start();
    }

    public void setupTestEnvironment() {
        super.setupTestEnvironment();
        global.eval( "var DT = "+
            "Packages.com.netscape.javascript.qa.liveconnect.DataTypeClass");
        global.eval( "var dt = new DT();" );            
    }

    public void    executeTest() {
        doArrayTest( "DT.staticGetShortArray();", true );
        doArrayTest( "DT.PUB_STATIC_ARRAY_SHORT;", true);
        doArrayTest( "dt.getShortArray();", true );
        doArrayTest( "dt.PUB_ARRAY_SHORT;", false );
    }
    /**
     *    Assign a java short[] to    a JavaScript variable in the following ways:
     *    <ul>
     *    <li>    Call a static method
     *    <li>    Get    the    value of a static field
     *    <li>    Call an    instance method
     *    <li>    Get    the    value of an    instance field.
     *
     *    @param command the command to eval to get the byte array
     */
    
    public void    doArrayTest( String    command, boolean shouldEqual    ) {
        short    array[]     = DataTypeClass.PUB_STATIC_ARRAY_SHORT;
        short   jsArray[];
        int        jsArray_length;
        
        try    {
            //    assign the array to    a JavaScript variable
            global.eval( "var jsArray =    " +    command    );
            
            // get the jsArray object, which should be the java object
            jsArray = (short[]) global.getMember( "jsArray" );

            // get the length of the array from JavaScript
            jsArray_length =
                ((Double) global.eval("jsArray.length")).intValue();

            //  iterate through jsArray in JavaScript. verify that the type and
            //  value of each object in the array is correct.

            for ( int i = 0; i < jsArray_length; i++ ) {
                //  verify that the array item is the same object as in the
                //  original array

                Double item = (Double) global.eval( "jsArray[" + i +"];" );

                addTestCase(
                    "[ jsArray = "    + command +"] "+                
                    "global.eval(\"jsArray["+i+"]\").equals( array["+i+"])",
                    "true",
                    (item.equals(new Double(array[i]))) +"",
                    "" );
            }

        } catch ( Exception e ) {
            e.printStackTrace();
            file.exception = e.toString();
            jsArray_length = 0;
            jsArray = null;
        }

        // verify that jsArray is the same as the original array

        addTestCase(
            "[jsArray = "+ command +"] "+        
            "jsArray = global.getMember( \"jsArray\"); "+
            "jsArray == array",
            ( shouldEqual ) ? "true" : "false",
            (jsArray == array )    +"",
            "" );
    }
 }