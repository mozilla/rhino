/* -*- Mode: java; tab-width:8; indent-tabs-mode: nil; c-basic-offset: 4 -*- */

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'regress-426520.js';

var summary = 'Do not crash @ ParseXMLSource';
var BUGNUMBER = 426520;
var actual = '';
var expect = '';

printBugNumber(BUGNUMBER);
START(summary);

undefined = {};

try
{
    with (this) {
        throw <x/>;
    }
}
catch(ex)
{
}

TEST(1, expect, actual);

END();
