/* -*- Mode: java; tab-width:8; indent-tabs-mode: nil; c-basic-offset: 4 -*- */

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'regress-458679-01.js';

var summary = 'GetXMLEntity should not assume FastAppendChar is infallible';
var BUGNUMBER = 458679;
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
START(summary);

try
{
    var x = "<";

    while (x.length < 12000000)
        x += x;

    <e4x>{x}</e4x>;
}
catch(ex)
{
}

TEST(1, expect, actual);

END();
