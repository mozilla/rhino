/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'switch-003.js';

/**
 *  File Name:          switch-003.js
 *  ECMA Section:
 *  Description:        The switch Statement
 *
 *  Attempt to verify that case statements are evaluated in source order
 *
 *  Author:             christine@netscape.com
 *  Date:               11 August 1998
 *
 */
var SECTION = "switch-003";
var VERSION = "ECMA_2";
var TITLE   = "The switch statement";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

SwitchTest( "a", "abc" );
SwitchTest( "b", "bc" );
SwitchTest( "c", "c" );
SwitchTest( "d", "*abc" );
SwitchTest( "v", "*abc" );
SwitchTest( "w", "w*abc" );
SwitchTest( "x", "xw*abc" );
SwitchTest( "y", "yxw*abc" );
SwitchTest( "z", "zyxw*abc" );
//    SwitchTest( new java.lang.String("z"), "*abc" );

test();

function SwitchTest( input, expect ) {
  var result = "";

  switch ( input ) {
  case "z": result += "z";
  case "y": result += "y";
  case "x": result += "x";
  case "w": result += "w";
  default: result += "*";
  case "a": result += "a";
  case "b": result += "b";
  case "c": result += "c";
  }

  new TestCase(
    SECTION,
    "switch with no breaks:  input is " + input,
    expect,
    result );
}
