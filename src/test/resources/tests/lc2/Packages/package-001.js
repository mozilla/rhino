/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'package-001.js';

/**
   File Name:      package-001.js
   Description:

   @author     christine@netscape.com
   @version    1.00
*/
var SECTION = "LiveConnect Packages";
var VERSION = "1_3";
var TITLE   = "LiveConnect Packages";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

//  All packages are of the type "object"
var E_TYPE = "object";

//  The JavaScript [[Class]] property for all Packages is "[JavaPackage <packagename>]"
var E_JSCLASS = "[JavaPackage ";

//  Create arrays of actual results (java_array) and
//  expected results (test_array).

var java_array = new Array();
var test_array = new Array();

var i = 0;

java_array[i] = new JavaValue(  java  );
test_array[i] = new TestValue(  "java" );
i++;

java_array[i] = new JavaValue(  java.awt  );
test_array[i] = new TestValue(  "java.awt" );
i++;

java_array[i] = new JavaValue(  java.beans  );
test_array[i] = new TestValue(  "java.beans" );
i++;

java_array[i] = new JavaValue(  java.io  );
test_array[i] = new TestValue(  "java.io" );
i++;

java_array[i] = new JavaValue(  java.lang  );
test_array[i] = new TestValue(  "java.lang" );
i++;

java_array[i] = new JavaValue(  java.math  );
test_array[i] = new TestValue(  "java.math" );
i++;

java_array[i] = new JavaValue(  java.net  );
test_array[i] = new TestValue(  "java.net" );
i++;

java_array[i] = new JavaValue(  java.rmi  );
test_array[i] = new TestValue(  "java.rmi" );
i++;

java_array[i] = new JavaValue(  java.text  );
test_array[i] = new TestValue(  "java.text" );
i++;

java_array[i] = new JavaValue(  java.util  );
test_array[i] = new TestValue(  "java.util" );
i++;

java_array[i] = new JavaValue(  Packages.javax  );
test_array[i] = new TestValue(  "Packages.javax" );
i++;

java_array[i] = new JavaValue(  Packages.javax.javascript  );
test_array[i] = new TestValue(  "Packages.javax.javascript" );
i++;

java_array[i] = new JavaValue(  Packages.javax.javascript.examples  );
test_array[i] = new TestValue(  "Packages.javax.javascript.examples" );
i++;

for ( i = 0; i < java_array.length; i++ ) {
  CompareValues( java_array[i], test_array[i] );

}

test();
function CompareValues( javaval, testval ) {
  //  Check typeof, which should be E_TYPE
  new TestCase( SECTION,
		"typeof (" + testval.description +")",
		testval.type,
		javaval.type );

  //  Check JavaScript class, which should be E_JSCLASS + the package name
  new TestCase( SECTION,
		"(" + testval.description +").getJSClass()",
		testval.jsclass,
		javaval.jsclass );
}
function JavaValue( value ) {
  this.value  = value;
  this.type   = typeof value;
  this.jsclass = value +""
    return this;
}
function TestValue( description ) {
  this.packagename = (description.substring(0, "Packages.".length) ==
		      "Packages.") ? description.substring("Packages.".length, description.length ) :
    description;

  this.description = description;
  this.type =  E_TYPE;
  this.jsclass = E_JSCLASS +  this.packagename +"]";
  return this;
}
