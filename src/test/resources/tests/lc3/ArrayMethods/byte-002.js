/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'byte-002.js';

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

var b = new java.lang.String("abcdefghijklmnopqrstuvwxyz").getBytes();

new TestCase(
  "var b = new java.lang.String(\"abcdefghijklmnopqrstuvwxyz\").getBytes(); b.valueOf()",
  b,
  b.valueOf() );

test();
