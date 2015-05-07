/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'regexp-literals-002.js';

/**
 *  File Name:          LexicalConventions/regexp-literals-002.js
 *  ECMA Section:       7.8.5
 *  Description:        Based on ECMA 2 Draft 8 October 1999
 *
 *  Author:             christine@netscape.com
 *  Date:               19 February 1999
 */
var SECTION = "LexicalConventions/regexp-literals-002.js";
var VERSION = "ECMA_2";
var TITLE   = "Regular Expression Literals";

startTest();

// A regular expression literal represents an object of type RegExp.

AddTestCase(
  "// A regular expression literal represents an object of type RegExp.",
  "true",
  (/x*/ instanceof RegExp).toString() );

test();
