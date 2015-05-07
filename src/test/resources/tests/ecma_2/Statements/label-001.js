/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'label-001.js';

/**
 *  File Name:          label-001.js
 *  ECMA Section:
 *  Description:        Labeled statements
 *
 *  Labeled break and continue within a for loop.
 *
 *
 *  Author:             christine@netscape.com
 *  Date:               11 August 1998
 */
var SECTION = "label-003";
var VERSION = "ECMA_2";
var TITLE   = "Labeled statements";

startTest();
writeHeaderToLog( SECTION + " "+ TITLE);

LabelTest(0, 0);
LabelTest(1, 1)
  LabelTest(-1, 1000);
LabelTest(false,  0);
LabelTest(true, 1);

test();

function LabelTest( limit, expect) {
woo: for ( var result = 0; result < 1000; result++ ) { if (result == limit) { break woo; } else { continue woo; } };

  new TestCase(
    SECTION,
    "break out of a labeled for loop: "+ limit,
    expect,
    result );
}

