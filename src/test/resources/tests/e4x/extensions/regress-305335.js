/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'regress-305335.js';

var summary = "Regression - XML instance methods should type check in " +
    "JS_GetPrivate()";
var BUGNUMBER = 305335;
var actual = 'No Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
START(summary);

var o = new Number(0);
o.__proto__ = XML();

try
{ 
    o.parent();
}
catch(e)
{
    printStatus('Exception: ' + e);
}

TEST(1, expect, actual);
END();
