/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'String-001.js';

/**
   File Name:      String-001.js
   Description:

   When accessing a Java field whose value is a java.lang.String,
   JavaScript should read the value as the JavaScript JavaObject,
   an object whose class is JavaObject.

   To test this:

   1.  Call a java method that returns a java.lang.String
   2.  Check the value of the returned object, which should be the value
   of the string
   3.  Check the type of the returned object, which should be "object"
   4.  Check the class of the object, using Object.prototype.toString,
   which should be "[object JavaObject]"
   5.  Check the class of the JavaObject, using getClass, and compare
   it to java.lang.Class.forName("java.lang.String");

   NOT DONE.  need a test class with a string field.

   @author     christine@netscape.com
   @version    1.00
*/


var SECTION = "LiveConnect";
var VERSION = "1_3";
var TITLE   = "Java Number Primitive to JavaScript Object";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

//  display test information

test();

function CheckType( et, at ) {
}
function CheckValue( ev, av ) {
}
