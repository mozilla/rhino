/* -*- Mode: java; tab-width:8; indent-tabs-mode: nil; c-basic-offset: 4 -*- */

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'regress-370372.js';

var BUGNUMBER = 370372;
var summary = 'with (xmllist) function::name assignments';
var actual = 'No Exception';
var expect = 'No Exception';

printBugNumber(BUGNUMBER);
START(summary);

var tests = [<></>, <><a/></>, <><a/><b/></>];

function do_one(x)
{
    x.function::f = Math.sin;
    with (x) {
        function::toString = function::f = function() { return "test"; };
    }

    if (String(x) !== "test")
        throw "Failed to set toString";

    if (x.f() !== "test")
        throw "Failed to set set function f";

    if (x.function::toString != x.function::f)
        throw "toString and f are different";
}


for (var i  = 0; i != tests.length; ++i)
    do_one(tests[i]);

TEST(1, expect, actual);
END();
