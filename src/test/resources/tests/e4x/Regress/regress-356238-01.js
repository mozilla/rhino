/* -*- Mode: java; tab-width:8; indent-tabs-mode: nil; c-basic-offset: 4 -*- */

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

gTestfile = 'regress-356238-01.js';

var BUGNUMBER = 356238;
var summary = 'bug 356238';
var actual = 'No Crash';
var expect = 'No Crash';

printBugNumber(BUGNUMBER);
START(summary);

try
{
	<xml/>.replace(1);
}
catch(ex)
{
}
TEST(1, expect, actual);
END();
