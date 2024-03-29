/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'regress-001.js';

/**
 *  File Name:          RegExp/regress-001.js
 *  ECMA Section:       N/A
 *  Description:        Regression test case:
 *  JS regexp anchoring on empty match bug
 *  http://bugzilla.mozilla.org/show_bug.cgi?id=2157
 *
 *  Author:             christine@netscape.com
 *  Date:               19 February 1999
 */
var SECTION = "RegExp/hex-001.js";
var VERSION = "ECMA_2";
var TITLE   = "JS regexp anchoring on empty match bug";
var BUGNUMBER = "2157";

startTest();

AddRegExpCases( /a||b/(''),
		"//a||b/('')",
		1,
		[''] );

test();

function AddRegExpCases( regexp, str_regexp, length, matches_array ) {

  AddTestCase(
    "( " + str_regexp + " ).length",
    regexp.length,
    regexp.length );


  for ( var matches = 0; matches < matches_array.length; matches++ ) {
    AddTestCase(
      "( " + str_regexp + " )[" + matches +"]",
      matches_array[matches],
      regexp[matches] );
  }
}
