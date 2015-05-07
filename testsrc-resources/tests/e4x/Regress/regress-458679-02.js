/* -*- Mode: java; tab-width:8; indent-tabs-mode: nil; c-basic-offset: 4 -*- */

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'regress-458679-02.js';

var summary = 'GetXMLEntity should not assume FastAppendChar is infallible';
var BUGNUMBER = 458679;
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
START(summary);

function stringOfLength(n)
{
    if (n == 0) {
        return "";
    } else if (n == 1) {
        return "<";
    } else {
        var r = n % 2;
        var d = (n - r) / 2;
        var y = stringOfLength(d);
        return y + y + stringOfLength(r);
    }    
}

try
{

    void stringOfLength(4435455);
    x = stringOfLength(14435455);
    <xxx>{x}</xxx>;
}
catch(ex)
{
}

TEST(1, expect, actual);

END();
