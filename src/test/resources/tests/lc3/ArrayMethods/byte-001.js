/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'byte-001.js';

/**
 *  java array objects "inherit" JS string methods.  verify that byte arrays
 *  can inherit JavaScript Array object methods
 *
 *
 */
var SECTION = "java array object inheritance JavaScript Array methods";
var VERSION = "1_4";
var TITLE   = "LiveConnect 3.0 " + SECTION;

startTest();

var a = new Array();

a[a.length] = new TestObject(
  "var b"+a.length+" = new java.lang.String(\"hello\").getBytes(); b"+a.length+".join() +''",
  "b"+a.length,
  "join",
  true,
  "104,101,108,108,111" );

a[a.length] = new TestObject(
  "var b"+a.length+" = new java.lang.String(\"JavaScript\").getBytes(); b"+a.length+".reverse().join() +''",
  "b"+a.length,
  "reverse",
  true,
  getCharValues("tpircSavaJ") );

a[a.length] = new TestObject(
  "var b"+a.length+" = new java.lang.String(\"JavaScript\").getBytes(); b"+a.length+".sort().join() +''",
  "b"+a.length,
  "sort",
  true,
  "105,112,114,116,118,74,83,97,97,99" );

a[a.length] = new TestObject(
  "var b"+a.length+" = new java.lang.String(\"JavaScript\").getBytes(); b"+a.length+".sort().join() +''",
  "b"+a.length,
  "sort",
  true,
  "105,112,114,116,118,74,83,97,97,99" );

test();

// given a string, return a string consisting of the char value of each
// character in the string, separated by commas

function getCharValues(string) {
  for ( var c = 0, cString = ""; c < string.length; c++ ) {
    cString += string.charCodeAt(c) + ((c+1 < string.length) ? "," : "");
  }
  return cString;
}

// figure out what methods exist
// if there is no java method with the same name as a js method, should
// be able to invoke the js method without casting to a js string.  also
// the method should equal the same method of String.prototype.
// if there is a java method with the same name as a js method, invoking
// the method should call the java method

function TestObject( description, ob, method, override, expect ) {
  this.description = description;
  this.object = ob;
  this.method = method;
  this.override = override
    this.expect;

  this.result = eval(description);

  this.isJSMethod = eval( ob +"."+ method +" == Array.prototype." + method );

  // verify result of method

  new TestCase(
    description,
    expect,
    this.result );

  // verify that method is the method of Array.prototype

  new TestCase(
    ob +"." + method +" == Array.prototype." + method,
    override,
    this.isJSMethod );

  // verify that it's not cast to JS Array type

  new TestCase(
    ob + ".getClass().getName() +''",
    "[B",
    eval( ob+".getClass().getName() +''") );

}
