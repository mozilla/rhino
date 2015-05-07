/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'proto_9.js';

/**
   File Name:          proto_9.js
   Section:
   Description:        Local versus Inherited Values

   This tests Object Hierarchy and Inheritance, as described in the document
   Object Hierarchy and Inheritance in JavaScript, last modified on 12/18/97
   15:19:34 on http://devedge.netscape.com/.  Current URL:
   http://devedge.netscape.com/docs/manuals/communicator/jsobj/contents.htm

   This tests the syntax ObjectName.prototype = new PrototypeObject using the
   Employee example in the document referenced above.

   This tests

   Author:             christine@netscape.com
   Date:               12 november 1997
*/

var SECTION = "proto_9";
var VERSION = "JS1_3";
var TITLE   = "Local versus Inherited Values";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

function Employee ( name, dept ) {
  this.name = name || "";
  this.dept = dept || "general";
}
function WorkerBee ( name, dept, projs ) {
  this.projects = new Array();
}
WorkerBee.prototype = new Employee();

var pat = new WorkerBee()

  Employee.prototype.specialty = "none";
Employee.prototype.name = "Unknown";

Array.prototype.getClass = Object.prototype.toString;

// Pat, the WorkerBee

new TestCase( SECTION,
	      "pat.name",
	      "",
	      pat.name );

new TestCase( SECTION,
	      "pat.dept",
	      "general",
	      pat.dept );

new TestCase( SECTION,
	      "pat.projects.getClass",
	      "[object Array]",
	      pat.projects.getClass() );

new TestCase( SECTION,
	      "pat.projects.length",
	      0,
	      pat.projects.length );

test();
