/* -*- Mode: java; tab-width:8; indent-tabs-mode: nil; c-basic-offset: 4 -*- */

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'regress-324422-1.js';

var summary = "Do not crash creating XML object with long initialiser";

var BUGNUMBER = 324422;
var actual = 'No Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
START(summary);

if (typeof document == 'undefined')
{
    printStatus ("Expect possible out of memory error");
    expectExitCode(0);
    expectExitCode(5);
}
var str = '<fu>x</fu>';

for (var icount = 0; icount < 20; icount++)
{
    str = str + str;
}

printStatus(str.length);

try
{
    var x = new XML('<root>' + str + '</root>');
}
catch(ex)
{
    expect = 'InternalError: script stack space quota is exhausted';
    actual = ex + '';
    print('Caught ' + ex);
}

TEST(1, expect, actual);

END();
