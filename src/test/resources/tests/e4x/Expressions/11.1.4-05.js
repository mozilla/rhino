/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = '11.1.4-05.js';

var summary = "11.1.4 - XML Initializer - Comment hiding parsing/scanning";
var BUGNUMBER = 311157;
var actual;
var expect;

printBugNumber(BUGNUMBER);
START(summary);

XML.ignoreWhitespace = false;

var x = <bye> <![CDATA[ duh ]]>
    there </bye>;

actual = x.toString();
expect = '  duh \n    there ';
TEST(1, expect, actual);

END();
