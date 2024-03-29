/* -*- Mode: javascript; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'hex-001.js';

/**
 *  File Name:          RegExp/hex-001.js
 *  ECMA Section:       15.7.3.1
 *  Description:        Based on ECMA 2 Draft 7 February 1999
 *  Positive test cases for constructing a RegExp object
 *  Author:             christine@netscape.com
 *  Date:               19 February 1999
 */
var SECTION = "RegExp/hex-001";
var VERSION = "ECMA_2";
var TITLE   = "RegExp patterns that contain HexicdecimalEscapeSequences";

startTest();

// These examples come from 15.7.1, HexidecimalEscapeSequence

AddRegExpCases( new RegExp("\x41"),  "new RegExp('\\x41')",  "A",  "A", 1, 0, ["A"] );
AddRegExpCases( new RegExp("\x412"),"new RegExp('\\x412')", "A2", "A2", 1, 0, ["A2"] );
AddRegExpCases( new RegExp("\x1g"), "new RegExp('\\x1g')",  "x1g","x1g", 1, 0, ["x1g"] );

AddRegExpCases( new RegExp("A"),  "new RegExp('A')",  "\x41",  "\\x41",  1, 0, ["A"] );
AddRegExpCases( new RegExp("A"),  "new RegExp('A')",  "\x412", "\\x412", 1, 0, ["A"] );
AddRegExpCases( new RegExp("^x"), "new RegExp('^x')", "x412",  "x412",   1, 0, ["x"]);
AddRegExpCases( new RegExp("A"),  "new RegExp('A')",  "A2",    "A2",     1, 0, ["A"] );

test();

function AddRegExpCases(
  regexp, str_regexp, pattern, str_pattern, length, index, matches_array ) {

  // prevent a runtime error

  if ( regexp.exec(pattern) == null || matches_array == null ) {
    AddTestCase(
      str_regexp + ".exec(" + pattern +")",
      matches_array,
      regexp.exec(pattern) );

    return;
  }

  AddTestCase(
    str_regexp + ".exec(" + str_pattern +").length",
    length,
    regexp.exec(pattern).length );

  AddTestCase(
    str_regexp + ".exec(" + str_pattern +").index",
    index,
    regexp.exec(pattern).index );

  AddTestCase(
    str_regexp + ".exec(" + str_pattern +").input",
    pattern,
    regexp.exec(pattern).input );

  for ( var matches = 0; matches < matches_array.length; matches++ ) {
    AddTestCase(
      str_regexp + ".exec(" + str_pattern +")[" + matches +"]",
      matches_array[matches],
      regexp.exec(pattern)[matches] );
  }
}
