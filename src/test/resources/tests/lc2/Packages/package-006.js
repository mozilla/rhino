/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'package-006.js';

/**
   File Name:      package-006.js
   Description:

   Access a package property that does not exist.

   @author     christine@netscape.com
   @version    1.00
*/

var SECTION = "LiveConnect Packages";
var VERSION = "1_3";
var TITLE   = "LiveConnect Packages";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

var util = java.util;
var v = new util.Vector();

new TestCase( SECTION,
	      "java.util[1]",
	      void 0,
	      java.util[1] );

test();

