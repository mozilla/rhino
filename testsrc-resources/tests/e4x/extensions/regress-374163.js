/* -*- Mode: java; tab-width:8; indent-tabs-mode: nil; c-basic-offset: 4 -*- */

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'regress-374163.js';

var BUGNUMBER = 374163;
var summary = 'Set E4X xml.function::__proto__ = null causes toString to throw';
var actual = '';
var expect = 'TypeError: String.prototype.toString called on incompatible XML';

printBugNumber(BUGNUMBER);
START(summary);

try
{
    var a = <a/>;
    a.function::__proto__ = null;
    "" + a;
}
catch(ex)
{
    actual = ex + '';
}

TEST(1, expect, actual);
END();
